#!/usr/bin/env python3
"""
MedicalAssistant — 接口性能 & 优化潜力测试脚本
================================================
测试维度：
  1. RAG 冷/热延迟（cache miss vs hit）
  2. Predict 延迟
  3. Agent chat 延迟
  4. 并发压测（bulkhead 边界）
  5. Neo4j 直查延迟（结构化知识召回潜力）
  6. Flask /rag/query 直接调用 vs Spring 代理调用对比

用法:
  python3 test/perf/perf_test.py [--base-url http://127.0.0.1:80] [--flask-url http://127.0.0.1:8001]
  python3 test/perf/perf_test.py --skip-neo4j   # 跳过 Neo4j 测试
"""
from __future__ import annotations
import argparse
import json
import statistics
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from typing import Optional


# ─── ANSI ──────────────────────────────────────────────────────────────────
GREEN = "\033[92m"; YELLOW = "\033[93m"; RED = "\033[91m"
CYAN  = "\033[96m"; BOLD   = "\033[1m";  RESET = "\033[0m"
def ok(s): return f"{GREEN}{s}{RESET}"
def warn(s): return f"{YELLOW}{s}{RESET}"
def err(s): return f"{RED}{s}{RESET}"
def h(s): return f"{BOLD}{CYAN}{s}{RESET}"


# ─── HTTP helpers ───────────────────────────────────────────────────────────
def post(url: str, payload: dict, timeout=90) -> tuple[int, dict, float]:
    body = json.dumps(payload, ensure_ascii=False).encode()
    req = urllib.request.Request(url, data=body,
                                  headers={"Content-Type": "application/json; charset=utf-8"},
                                  method="POST")
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            ms = (time.perf_counter() - t0) * 1000
            return r.status, json.loads(r.read().decode()), ms
    except urllib.error.HTTPError as e:
        ms = (time.perf_counter() - t0) * 1000
        try:
            body = json.loads(e.read().decode())
        except Exception:
            body = {"error": str(e)}
        return e.code, body, ms
    except Exception as e:
        ms = (time.perf_counter() - t0) * 1000
        return 0, {"error": str(e)}, ms


def get(url: str, timeout=10) -> tuple[int, dict, float]:
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=timeout) as r:
            ms = (time.perf_counter() - t0) * 1000
            return r.status, json.loads(r.read().decode()), ms
    except Exception as e:
        ms = (time.perf_counter() - t0) * 1000
        return 0, {"error": str(e)}, ms


def neo4j_query(cypher: str, params: dict | None = None, host="localhost",
                user="neo4j", password="password") -> tuple[list, float]:
    url = f"http://{host}:7474/db/neo4j/tx/commit"
    payload = {"statements": [{"statement": cypher, "parameters": params or {}}]}
    body = json.dumps(payload).encode()
    creds = f"{user}:{password}"
    import base64
    auth = base64.b64encode(creds.encode()).decode()
    req = urllib.request.Request(url, data=body,
                                  headers={"Content-Type": "application/json",
                                           "Authorization": f"Basic {auth}"},
                                  method="POST")
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            ms = (time.perf_counter() - t0) * 1000
            data = json.loads(r.read().decode())
            rows = [row["row"] for row in data["results"][0]["data"]]
            return rows, ms
    except Exception as e:
        return [], (time.perf_counter() - t0) * 1000


# ─── Stats ──────────────────────────────────────────────────────────────────
@dataclass
class Stats:
    name: str
    times: list[float] = field(default_factory=list)
    errors: int = 0

    def add(self, ms: float, ok: bool = True):
        self.times.append(ms)
        if not ok:
            self.errors += 1

    def report(self):
        if not self.times:
            return f"  {self.name}: no data"
        s = self.times
        p = lambda pct: sorted(s)[int(len(s) * pct / 100)]
        return (f"  n={len(s)} err={self.errors}  "
                f"avg={statistics.mean(s):.0f}ms  "
                f"p50={p(50):.0f}ms  p90={p(90):.0f}ms  "
                f"p95={p(95):.0f}ms  p99={p(99):.0f}ms  "
                f"min={min(s):.0f}ms  max={max(s):.0f}ms")


# ─── Test cases ─────────────────────────────────────────────────────────────
RAG_QUESTIONS = [
    "布洛芬有哪些常见不良反应？",
    "阿司匹林和华法林联用有什么风险？",
    "二甲双胍常见副作用有哪些？",
    "孕妇能服用阿司匹林吗？",
    "高血压患者日常用药管理要注意什么？",
]

PREDICT_CASES = [
    "Patient: 68-year-old with hypertension. Taking ibuprofen for joint pain.",
    "72-year-old female on warfarin for atrial fibrillation. Recently started aspirin 100mg daily.",
    "65-year-old male with Type 2 diabetes and stage 3 CKD. Taking metformin 500mg twice daily.",
]

NEO4J_DRUG_QUERIES = [
    ("布洛芬", "ibuprofen"),
    ("阿司匹林", "aspirin"),
    ("二甲双胍", "metformin"),
    ("华法林", "warfarin"),
]


def section(title):
    print(f"\n{h('═'*60)}")
    print(f"{h(title)}")
    print(f"{h('═'*60)}")


# ─── 1. Health check ─────────────────────────────────────────────────────────
def test_health(base_url, flask_url):
    section("1. 服务健康检查")
    for name, url in [
        ("Spring Agent", f"{base_url}/api/agent/health"),
        ("Flask /health", f"{flask_url}/health"),
    ]:
        status, body, ms = get(url)
        sym = ok("✓") if status == 200 else err("✗")
        print(f"  {sym} {name}  {status}  {ms:.0f}ms  {body.get('status','?')}")


# ─── 2. RAG 冷/热延迟 ────────────────────────────────────────────────────────
def test_rag_latency(base_url, flush_cache=True):
    section("2. RAG 冷/热延迟（Spring 代理 → Flask → DashScope）")

    # 可选：清缓存（Redis FLUSHDB 太危险，改为用不同 question 避免命中）
    cold_stats = Stats("cold")
    warm_stats = Stats("warm")

    url = f"{base_url}/api/rag/query"
    for q in RAG_QUESTIONS:
        payload = {"question": q + " [perf_cold]", "top_k": 3, "with_timing": True}
        # 冷请求
        status, body, ms = post(url, payload)
        success = status == 200 and body.get("success")
        cache_hit = body.get("cache_hit", False)
        cold_stats.add(ms, success)
        sym = ok("✓") if success else err("✗")
        hit_tag = warn("CACHE") if cache_hit else "MISS "
        print(f"  {sym} COLD {hit_tag}  {ms:7.0f}ms  {q[:28]}")

        # 热请求（同一问题，应命中 Redis）
        status2, body2, ms2 = post(url, payload)
        cache_hit2 = body2.get("cache_hit", False)
        warm_stats.add(ms2, status2 == 200 and body2.get("success"))
        hit_tag2 = ok(" HIT") if cache_hit2 else warn("MISS")
        print(f"  {'  '} WARM {hit_tag2}  {ms2:7.0f}ms  (repeat)")

    print(f"\n  {BOLD}COLD{RESET} {cold_stats.report()}")
    print(f"  {BOLD}WARM{RESET} {warm_stats.report()}")
    speedup = statistics.mean(cold_stats.times) / max(1, statistics.mean(warm_stats.times))
    print(f"  {ok(f'Cache speedup: {speedup:.0f}x')}")


# ─── 3. Flask 直接调用 vs Spring 代理 ───────────────────────────────────────
def test_rag_proxy_overhead(base_url, flask_url):
    section("3. Spring 代理开销（Flask 直达 vs 经过 Spring RAG service）")
    q = "对乙酰氨基酚用药时需要注意什么？ [overhead_test]"
    payload_spring = {"question": q, "top_k": 3}
    payload_flask  = {"question": q, "top_k": 3, "strategy": "hybrid", "with_timing": True}

    spring_times, flask_times = [], []
    for i in range(3):
        _, _, ms_s = post(f"{base_url}/api/rag/query", payload_spring)
        _, _, ms_f = post(f"{flask_url}/rag/query", payload_flask)
        spring_times.append(ms_s)
        flask_times.append(ms_f)
        print(f"  run{i+1}  Spring={ms_s:7.0f}ms  Flask={ms_f:7.0f}ms  "
              f"overhead={ms_s-ms_f:+.0f}ms")

    avg_s = statistics.mean(spring_times)
    avg_f = statistics.mean(flask_times)
    print(f"\n  avg Spring={avg_s:.0f}ms  Flask={avg_f:.0f}ms  proxy_overhead={avg_s-avg_f:+.0f}ms")


# ─── 4. Predict 延迟 ─────────────────────────────────────────────────────────
def test_predict(base_url, flask_url):
    section("4. Predict /api/predict/analyze 延迟")
    stats_s = Stats("spring")
    stats_f = Stats("flask")
    for text in PREDICT_CASES:
        payload = {"text": text}
        sc, bs, ms_s = post(f"{base_url}/api/predict/analyze", payload)
        sf, bf, ms_f = post(f"{flask_url}/api/predict/analyze", payload)
        ok_s = sc == 200 and bs.get("status") == "success"
        ok_f = sf == 200 and bf.get("status") == "success"
        stats_s.add(ms_s, ok_s)
        stats_f.add(ms_f, ok_f)
        preds = [p["reaction"] for p in bs.get("predictions", [])[:3]] if ok_s else ["ERR"]
        print(f"  Spring={ms_s:6.0f}ms Flask={ms_f:6.0f}ms  top3={preds}")
    print(f"\n  Spring {stats_s.report()}")
    print(f"  Flask  {stats_f.report()}")


# ─── 5. Agent chat 延迟 ──────────────────────────────────────────────────────
def test_agent(base_url):
    section("5. Agent /api/agent/chat 延迟")
    cases = [
        ("血压监测", "请用一句话说明高血压患者监测血压的注意事项。"),
        ("药物查询", "布洛芬的主要不良反应是什么？"),
        ("用药提醒", "我每天需要吃两次二甲双胍，最重要的注意事项是什么？"),
    ]
    stats = Stats("agent")
    for label, msg in cases:
        payload = {"user_id": "99999", "session_id": f"perf-{label}", "message": msg}
        status, body, ms = post(f"{base_url}/api/agent/chat", payload, timeout=60)
        success = status == 200 and body.get("success")
        stats.add(ms, success)
        reply = str(body.get("assistant_message", body.get("error", "")))[:50]
        sym = ok("✓") if success else err("✗")
        print(f"  {sym} [{label}] {ms:7.0f}ms  → {reply}")
    print(f"\n  {stats.report()}")


# ─── 6. 并发压测 ─────────────────────────────────────────────────────────────
def test_concurrency(base_url, levels=(1, 4, 8, 16)):
    section("6. 并发压测（RAG 缓存命中 — 测 Spring 吞吐）")
    # 先预热缓存
    q = "辛伐他汀服用期间有哪些注意事项？ [concurrency]"
    payload = {"question": q, "top_k": 3}
    post(f"{base_url}/api/rag/query", payload, timeout=60)  # warm cache

    url = f"{base_url}/api/rag/query"
    for n in levels:
        times = []
        errors = 0
        t_start = time.perf_counter()
        with ThreadPoolExecutor(max_workers=n) as exe:
            futs = [exe.submit(post, url, payload, 30) for _ in range(n * 3)]
            for f in as_completed(futs):
                status, _, ms = f.result()
                times.append(ms)
                if status != 200:
                    errors += 1
        wall = (time.perf_counter() - t_start) * 1000
        rps = len(times) / (wall / 1000)
        avg = statistics.mean(times) if times else 0
        p95 = sorted(times)[int(len(times) * 0.95)] if len(times) > 1 else avg
        sym = ok("✓") if errors == 0 else warn(f"err={errors}")
        print(f"  conc={n:2d}  reqs={len(times):3d}  {sym}  "
              f"avg={avg:6.0f}ms  p95={p95:6.0f}ms  rps={rps:.1f}")


# ─── 7. Neo4j 直查延迟 & 药物知识探测 ─────────────────────────────────────────
def test_neo4j(neo4j_host="localhost"):
    section("7. Neo4j 知识图谱直查（药物不良反应 — 未接入 RAG 的潜在加速源）")

    # Schema 概览
    rows, ms = neo4j_query(
        "MATCH (n) RETURN labels(n)[0] as label, count(n) as cnt ORDER BY cnt DESC",
        host=neo4j_host)
    if not rows:
        print(err("  ✗ Neo4j 连接失败（bolt/http 7474 不可达）"))
        return
    print(f"  Schema 查询  {ms:.0f}ms")
    for r in rows[:6]:
        print(f"    {r[0]:20s} {r[1]:>8,}")

    print()
    # 药物→不良反应 查询
    stats = Stats("neo4j_drug_reactions")
    for cn_name, en_name in NEO4J_DRUG_QUERIES:
        cypher = """
        MATCH (d:Drug)-[:CAUSES_REACTION]->(r:Reaction)
        WHERE toLower(d.primaryid) CONTAINS $name OR toLower(d.name) CONTAINS $name
        RETURN r.pt as reaction, count(*) as freq
        ORDER BY freq DESC LIMIT 5
        """
        rows, ms = neo4j_query(cypher, {"name": en_name}, host=neo4j_host)
        stats.add(ms, len(rows) > 0)
        sym = ok("✓") if rows else warn("0 rows")
        reactions = [r[0] for r in rows[:3]] if rows else []
        print(f"  {sym} {cn_name:5s}({en_name:12s})  {ms:5.0f}ms  top3={reactions}")

    # 药物相互作用查询
    print()
    cypher2 = """
    MATCH (d1:Drug)-[:USED_IN_CASE]->(c)<-[:USED_IN_CASE]-(d2:Drug)
    WHERE toLower(d1.name) CONTAINS 'aspirin' AND toLower(d2.name) CONTAINS 'warfarin'
    WITH c
    MATCH (c)-[:CAUSES_REACTION]->(r:Reaction)
    RETURN r.pt as reaction, count(*) as freq ORDER BY freq DESC LIMIT 5
    """
    rows2, ms2 = neo4j_query(cypher2, host=neo4j_host)
    print(f"  阿司匹林+华法林 共患反应查询  {ms2:.0f}ms  {len(rows2)} 条")
    for r in rows2[:5]:
        print(f"    {r[0]:30s}  {r[1]:>5} cases")

    print(f"\n  {stats.report()}")
    print(f"\n  {BOLD}优化建议{RESET}: Neo4j 查询 p50 < 100ms，可在 Flask /rag/query 中")
    print(f"  预先召回结构化药物事实注入 LLM prompt，减少 hallucination 并提升准确率。")


# ─── 8. 综合摘要 ─────────────────────────────────────────────────────────────
def print_summary(base_url, flask_url):
    section("8. 综合瓶颈分析")
    lines = [
        ("LLM 冷调用",       "15–25 s",  "DashScope qwen-plus 网络 RTT + 推理时间；不可消除"),
        ("Redis 缓存命中",   "< 5 ms",   "当前已实现，重复问题无需回 LLM"),
        ("Spring 代理开销",  "< 10 ms",  "HTTP 代理层极轻，可忽略"),
        ("Predict 延迟",     "2–6 s",    "每次走 LLM，无缓存；可加 Redis 缓存 clinical summary hash"),
        ("Agent 延迟",       "4–30 s",   "多轮工具调用累计；MedicalAgent 调用 DashScope 多次"),
        ("Neo4j 未接入",     "—",        "175k Drug + 186k Reaction 闲置；接入可减少 LLM token 消耗"),
    ]
    print(f"  {'组件':<20} {'延迟':<12} {'说明'}")
    print(f"  {'─'*20} {'─'*12} {'─'*42}")
    for comp, lat, note in lines:
        print(f"  {comp:<20} {lat:<12} {note}")

    print(f"""
  {BOLD}TOP 3 优化项:{RESET}
  {ok('①')} Neo4j RAG 增强 — Flask /rag/query 先查图谱获取 structured facts，
     注入 prompt，可把 LLM context 从"凭记忆"变为"基于证据"，
     准确率提升且 token 减少（更短问题 → 更快推理）。

  {ok('②')} Predict 缓存 — 对 text 做 SHA-256 作 Redis key，TTL 1800s，
     重复临床摘要直接命中，latency 从 3s → <5ms。

  {ok('③')} 模型降级 — application.yaml 已配 qwen-turbo（比 qwen-plus 快 2-3x），
     但 flask/app.py 中 RAG_MODEL = "qwen-plus"，两者不统一；
     建议 Flask 也改为 qwen-turbo，或通过环境变量统一。
""")


# ─── main ────────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url",   default="http://127.0.0.1:80")
    parser.add_argument("--flask-url",  default="http://127.0.0.1:8001")
    parser.add_argument("--neo4j-host", default="localhost")
    parser.add_argument("--skip-neo4j", action="store_true")
    parser.add_argument("--skip-agent", action="store_true")
    parser.add_argument("--skip-cold",  action="store_true", help="跳过 LLM 冷调用（节省时间）")
    args = parser.parse_args()

    print(f"\n{BOLD}MedicalAssistant 性能测试{RESET}")
    print(f"  Spring : {args.base_url}")
    print(f"  Flask  : {args.flask_url}")
    print(f"  Neo4j  : {args.neo4j_host}:7474")

    test_health(args.base_url, args.flask_url)

    if not args.skip_cold:
        test_rag_latency(args.base_url)
        test_rag_proxy_overhead(args.base_url, args.flask_url)
        test_predict(args.base_url, args.flask_url)

    if not args.skip_agent:
        test_agent(args.base_url)

    test_concurrency(args.base_url)

    if not args.skip_neo4j:
        test_neo4j(args.neo4j_host)

    print_summary(args.base_url, args.flask_url)


if __name__ == "__main__":
    main()
