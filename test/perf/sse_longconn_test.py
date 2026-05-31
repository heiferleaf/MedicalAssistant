#!/usr/bin/env python3
"""
SSE 长连接专项测试
验证目标：
  1. 连接建立后立即收到 queued 事件（<500ms）
  2. 心跳每 ~15s 到达一次，不得断连
  3. 连接在 20s+ 后仍然存活（nginx 120s proxy_read_timeout 不中断）
  4. 最终 message + end 事件正确送达
  5. 50 并发 SSE 连接全部稳定
"""

import requests
import threading
import time
import statistics
import json
import sys
from datetime import datetime

BASE_URL    = "http://localhost"
SESSION_URL = f"{BASE_URL}/api/agent/sessions"
STREAM_URL  = f"{BASE_URL}/api/agent/chat/stream"

CONCURRENT        = 50    # 并发连接数
CONN_TIMEOUT_S    = 60    # 单连接最大等待（稍大于 20s mock 延迟）
EXPECTED_DELAY_S  = 20    # mock response-delay-ms=20000
HEARTBEAT_TARGET  = 15    # 预期心跳间隔（秒）
HEARTBEAT_DRIFT_S = 3     # 允许的最大漂移

QUESTIONS = [
    "我最近头疼，应该怎么办？",
    "高血压患者可以服用哪些降压药？",
    "如何判断是否需要去医院急诊？",
    "孕期能吃感冒药吗？",
    "糖尿病患者的饮食注意事项有哪些？",
    "青霉素过敏的人有哪些替代抗生素？",
    "长期失眠怎么治疗？",
    "儿童发烧超过多少度需要去医院？",
    "腰椎间盘突出如何保守治疗？",
    "老年人补钙应该注意什么？",
]

results = []
lock = threading.Lock()


def create_session(user_id: str) -> str | None:
    try:
        resp = requests.post(
            SESSION_URL,
            json={"userId": user_id},
            timeout=10,
        )
        data = resp.json()
        return data.get("data", {}).get("sessionId")
    except Exception as e:
        return None


def parse_sse_stream(resp):
    """Yield (event_name, data, timestamp) tuples from a streaming response."""
    event_name = None
    for raw_line in resp.iter_lines(decode_unicode=True):
        ts = time.time()
        line = raw_line.strip() if raw_line else ""
        if line.startswith("event:"):
            event_name = line[len("event:"):].strip()
        elif line.startswith("data:"):
            data = line[len("data:"):].strip()
            yield event_name, data, ts
            event_name = None
        elif line == "" and event_name:
            # field-less event (just event: name with no data)
            yield event_name, "", ts
            event_name = None


def run_sse_connection(thread_id: int):
    user_id    = f"sse_lt_{thread_id}"
    question   = QUESTIONS[thread_id % len(QUESTIONS)]
    session_id = create_session(user_id)

    if not session_id:
        with lock:
            results.append({
                "thread": thread_id,
                "error": "session creation failed",
            })
        return

    params = {
        "user_id":    user_id,
        "session_id": session_id,
        "message":    question,
    }

    t_start = time.time()
    events  = []          # list of (event_name, data, elapsed_s)
    error   = None

    try:
        with requests.get(
            STREAM_URL,
            params=params,
            stream=True,
            timeout=CONN_TIMEOUT_S,
            headers={"Accept": "text/event-stream", "Cache-Control": "no-cache"},
        ) as resp:
            if resp.status_code != 200:
                with lock:
                    results.append({
                        "thread":   thread_id,
                        "error":    f"HTTP {resp.status_code}",
                        "events":   [],
                    })
                return

            for evt_name, data, ts in parse_sse_stream(resp):
                elapsed = ts - t_start
                events.append((evt_name, data, elapsed))
                if evt_name == "end":
                    break

    except requests.exceptions.Timeout:
        error = f"timeout after {CONN_TIMEOUT_S}s"
    except Exception as e:
        error = str(e)

    t_end    = time.time()
    duration = t_end - t_start

    heartbeat_ts = [elapsed for (n, _, elapsed) in events if n == "heartbeat"]
    hb_intervals = [heartbeat_ts[i] - heartbeat_ts[i-1]
                    for i in range(1, len(heartbeat_ts))]

    def first_elapsed(event_name):
        for n, _, elapsed in events:
            if n == event_name:
                return elapsed
        return None

    with lock:
        results.append({
            "thread":         thread_id,
            "session_id":     session_id,
            "duration_s":     round(duration, 2),
            "ttfb_ms":        round((first_elapsed("queued") or 0) * 1000, 1),
            "has_queued":     any(n == "queued"    for n, _, _ in events),
            "has_message":    any(n == "message"   for n, _, _ in events),
            "has_end":        any(n == "end"       for n, _, _ in events),
            "hb_count":       len(heartbeat_ts),
            "hb_intervals_s": [round(x, 2) for x in hb_intervals],
            "t_first_hb_s":   round(heartbeat_ts[0],  2) if heartbeat_ts else None,
            "t_message_s":    round(first_elapsed("message") or 0, 2),
            "event_sequence": [n for n, _, _ in events],
            "error":          error,
        })


# ────────────────────────────────────────────────────────────────────────────
print(f"{'='*60}")
print(f"SSE 长连接专项测试")
print(f"并发数: {CONCURRENT}  期望延迟: {EXPECTED_DELAY_S}s  心跳目标: {HEARTBEAT_TARGET}s")
print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print(f"{'='*60}")

# Quick sanity check: 1 connection
print("\n[1/3] 单连接冒烟测试...")
smoke = []
def _smoke():
    user_id    = "sse_smoke"
    session_id = create_session(user_id)
    if not session_id:
        print("  ✗ session creation failed")
        return
    params = {"user_id": user_id, "session_id": session_id,
              "message": "头痛发烧该怎么办？"}
    t0 = time.time()
    with requests.get(STREAM_URL, params=params, stream=True,
                      timeout=CONN_TIMEOUT_S,
                      headers={"Accept": "text/event-stream"}) as resp:
        for evt, data, ts in parse_sse_stream(resp):
            smoke.append((evt, round(ts-t0, 2)))
            print(f"  +{ts-t0:5.2f}s  event={evt!r:<12} data={data[:50]!r}")
            if evt == "end":
                break

_smoke()
if not any(e == "queued" for e, _ in smoke):
    print("  ✗ 未收到 queued 事件")
elif not any(e == "end" for e, _ in smoke):
    print("  ✗ 未收到 end 事件")
else:
    print(f"  ✓ 冒烟测试通过，事件序列: {[e for e,_ in smoke]}")

# ────────────────────────────────────────────────────────────────────────────
print(f"\n[2/3] 启动 {CONCURRENT} 并发 SSE 连接...")
t_all_start = time.time()
threads = [threading.Thread(target=run_sse_connection, args=(i,)) for i in range(CONCURRENT)]
for i, t in enumerate(threads):
    t.start()
    time.sleep(0.03)   # 30ms stagger → ~1.5s ramp

for t in threads:
    t.join(timeout=CONN_TIMEOUT_S + 20)

t_all_elapsed = time.time() - t_all_start
print(f"所有线程结束，耗时 {t_all_elapsed:.1f}s")

# ────────────────────────────────────────────────────────────────────────────
print(f"\n[3/3] 结果分析")
print(f"{'='*60}")

ok  = [r for r in results if not r.get("error")]
err = [r for r in results if r.get("error")]

print(f"\n▸ 连接成功率: {len(ok)}/{len(results)} = {len(ok)/len(results)*100:.1f}%")
if err:
    print(f"▸ 失败原因:")
    for r in err[:5]:
        print(f"    thread={r['thread']}: {r['error']}")

if ok:
    # TTFB
    ttfb = [r["ttfb_ms"] for r in ok if r["ttfb_ms"] is not None]
    print(f"\n▸ TTFB（到首个 queued 事件）")
    print(f"    avg={statistics.mean(ttfb):.0f}ms  "
          f"median={statistics.median(ttfb):.0f}ms  "
          f"p90={sorted(ttfb)[int(len(ttfb)*.9)]:.0f}ms  "
          f"max={max(ttfb):.0f}ms")

    # Duration
    durations = [r["duration_s"] for r in ok]
    print(f"\n▸ 连接持续时间")
    print(f"    avg={statistics.mean(durations):.1f}s  "
          f"median={statistics.median(durations):.1f}s  "
          f"min={min(durations):.1f}s  "
          f"max={max(durations):.1f}s")
    long_enough = sum(1 for d in durations if d >= EXPECTED_DELAY_S * 0.9)
    print(f"    >= {EXPECTED_DELAY_S*0.9:.0f}s: {long_enough}/{len(ok)} "
          f"({'✓' if long_enough == len(ok) else '✗'})")

    # Heartbeat
    hb_counts = [r["hb_count"] for r in ok]
    all_intervals = [iv for r in ok for iv in r.get("hb_intervals_s", [])]
    print(f"\n▸ 心跳统计（目标每 {HEARTBEAT_TARGET}s）")
    print(f"    每连接心跳数: avg={statistics.mean(hb_counts):.1f}  "
          f"min={min(hb_counts)}  max={max(hb_counts)}")
    if all_intervals:
        avg_iv  = statistics.mean(all_intervals)
        max_iv  = max(all_intervals)
        within  = sum(1 for iv in all_intervals if abs(iv - HEARTBEAT_TARGET) <= HEARTBEAT_DRIFT_S)
        print(f"    间隔: avg={avg_iv:.1f}s  max={max_iv:.1f}s  "
              f"在 {HEARTBEAT_TARGET}±{HEARTBEAT_DRIFT_S}s 内: "
              f"{within}/{len(all_intervals)} "
              f"({'✓' if within == len(all_intervals) else '✗ 部分漂移'})")
    else:
        print("    无心跳区间数据（连接时间可能太短）")

    # Event coverage
    q_ok = sum(1 for r in ok if r["has_queued"])
    m_ok = sum(1 for r in ok if r["has_message"])
    e_ok = sum(1 for r in ok if r["has_end"])
    print(f"\n▸ 事件覆盖率")
    print(f"    queued:  {q_ok}/{len(ok)} ({'✓' if q_ok == len(ok) else '✗'})")
    print(f"    message: {m_ok}/{len(ok)} ({'✓' if m_ok == len(ok) else '✗'})")
    print(f"    end:     {e_ok}/{len(ok)} ({'✓' if e_ok == len(ok) else '✗'})")

    # First heartbeat timing
    first_hb = [r["t_first_hb_s"] for r in ok if r["t_first_hb_s"] is not None]
    if first_hb:
        print(f"\n▸ 首次心跳时刻（期望 ~{HEARTBEAT_TARGET}s）")
        print(f"    avg={statistics.mean(first_hb):.1f}s  "
              f"min={min(first_hb):.1f}s  max={max(first_hb):.1f}s")

    # Sample event sequences
    print(f"\n▸ 事件序列样本（前5个连接）")
    for r in ok[:5]:
        print(f"    thread={r['thread']:3d}  {r['event_sequence']}")

# Save JSON for archival
out_path = "/Users/mac/Desktop/project/MedicalAssistant/test/perf/sse_test_results.json"
with open(out_path, "w") as f:
    json.dump({
        "timestamp": datetime.now().isoformat(),
        "concurrent": CONCURRENT,
        "mock_delay_s": EXPECTED_DELAY_S,
        "results": results,
    }, f, indent=2, ensure_ascii=False)
print(f"\n原始数据已保存: {out_path}")
print(f"{'='*60}")
