"""Medical AI FastAPI Service — RAG / Predict / OCR via DashScope (Qwen).

Enhancements:
- Neo4j drug-reaction graph augmentation for /rag/query
- Redis predict cache to avoid redundant LLM calls
- Unified model: qwen-turbo (matches Spring application.yaml)
"""
import base64
import hashlib
import json
import logging
import os
import re
import time
import traceback
import urllib.request
from typing import Any, Optional

import dashscope
from dashscope import Generation, MultiModalConversation
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

app = FastAPI(title="Medical AI Service", version="1.0.0")

dashscope.api_key = os.environ.get("DASHSCOPE_API_KEY", "sk-e7051a4ddaa049e9bd25c8264dfb3b15")

RAG_MODEL     = "qwen-turbo"   # unified with Spring application.yaml
OCR_MODEL     = "qwen-vl-plus"

# ── Neo4j config ──────────────────────────────────────────────────────────────
NEO4J_HOST     = os.environ.get("NEO4J_HOST", "neo4j-medical")
NEO4J_PORT     = int(os.environ.get("NEO4J_PORT", "7474"))
NEO4J_USER     = os.environ.get("NEO4J_USER", "neo4j")
NEO4J_PASSWORD = os.environ.get("NEO4J_PASSWORD", "password")
NEO4J_ENABLED  = os.environ.get("NEO4J_ENABLED", "true").lower() == "true"

# ── Redis config ──────────────────────────────────────────────────────────────
REDIS_HOST         = os.environ.get("REDIS_HOST", "medical-ms-redis")
REDIS_PORT         = int(os.environ.get("REDIS_PORT", "6379"))
PREDICT_CACHE_TTL  = int(os.environ.get("PREDICT_CACHE_TTL", "1800"))
PREDICT_CACHE_ENABLED = os.environ.get("PREDICT_CACHE_ENABLED", "true").lower() == "true"

_redis_client: Any = None

def _get_redis():
    global _redis_client
    if _redis_client is not None:
        return _redis_client
    try:
        import redis as redis_lib
        _redis_client = redis_lib.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0,
                                        socket_connect_timeout=2, socket_timeout=2,
                                        decode_responses=True)
        _redis_client.ping()
        logger.info("Redis connected: %s:%d", REDIS_HOST, REDIS_PORT)
    except Exception as e:
        logger.warning("Redis unavailable (%s), predict cache disabled", e)
        _redis_client = None
    return _redis_client


RAG_SYSTEM = (
    "你是一名专业的医学知识助手，具有丰富的药理学、临床医学和药物安全知识。"
    "请根据医学文献和临床指南准确、简明地回答用户的医学问题。"
    "回答要点：基于循证医学证据；涉及药物时说明不良反应/禁忌/注意事项；"
    "语言清晰专业；超出医学范畴时建议咨询医生；直接作答，不引用知识库等系统词汇。"
)

PREDICT_SYSTEM = (
    "你是一名临床药理学专家，专门分析药物不良反应风险。"
    "根据临床摘要识别可能的药物不良反应，只返回JSON数组，不要任何其他文字。"
    'JSON格式: [{"reaction":"不良反应名称","probability":0.85}]'
    "要求：最多5条，按概率降序，probability范围0.0~1.0。"
)

# Drug name lookup: Chinese → English canonical name in Neo4j
DRUG_NAME_MAP: dict[str, list[str]] = {
    "布洛芬": ["IBUPROFEN"],
    "阿司匹林": ["ASPIRIN", "BAYER ASPIRIN", "ASPIRIN DL-LYSINE"],
    "华法林": ["WARFARIN"],
    "二甲双胍": ["METFORMIN"],
    "对乙酰氨基酚": ["ACETAMINOPHEN", "PARACETAMOL"],
    "氨氯地平": ["AMLODIPINE"],
    "左氧氟沙星": ["LEVOFLOXACIN"],
    "环丙沙星": ["CIPROFLOXACIN"],
    "辛伐他汀": ["SIMVASTATIN"],
    "阿托伐他汀": ["ATORVASTATIN"],
    "地高辛": ["DIGOXIN"],
    "奥美拉唑": ["OMEPRAZOLE"],
    "氨基糖苷": ["AMIKACIN", "GENTAMICIN"],
    "卡托普利": ["CAPTOPRIL"],
    "美托洛尔": ["METOPROLOL"],
    "氯吡格雷": ["CLOPIDOGREL"],
    "胰岛素": ["INSULIN"],
    "地塞米松": ["DEXAMETHASONE"],
    "泼尼松": ["PREDNISONE"],
    "甲氨蝶呤": ["METHOTREXATE"],
}


# ── Request / Response models ─────────────────────────────────────────────────

class RagRequest(BaseModel):
    question: str = ""
    top_k: int = 5
    strategy: str = "hybrid"
    knowledge_base_version: str = "default"
    with_trace: bool = False
    with_timing: bool = False


class PredictRequest(BaseModel):
    text: str = ""


# ── Neo4j helpers ─────────────────────────────────────────────────────────────

def _neo4j_post(cypher: str, params: dict | None = None) -> list:
    """Execute a Cypher query via HTTP transactional API."""
    url = f"http://{NEO4J_HOST}:{NEO4J_PORT}/db/neo4j/tx/commit"
    payload = {"statements": [{"statement": cypher, "parameters": params or {}}]}
    body = json.dumps(payload).encode()
    creds = base64.b64encode(f"{NEO4J_USER}:{NEO4J_PASSWORD}".encode()).decode()
    req = urllib.request.Request(
        url, data=body,
        headers={"Content-Type": "application/json", "Authorization": f"Basic {creds}"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=3) as r:
        data = json.loads(r.read().decode())
    results = data.get("results", [])
    if not results or not results[0].get("data"):
        return []
    return [row["row"] for row in results[0]["data"]]


def _query_drug_reactions(drug_names: list[str], limit: int = 8) -> list[str]:
    """Return top adverse reactions for the given drug names from Neo4j."""
    cypher = """
    MATCH (ds:DrugSet)-[:CONTAINS_DRUG]->(d:Drug)
    WHERE d.drugname IN $names
    WITH ds
    MATCH (ds)-[:CAUSES_REACTION]->(r:Reaction)
    WHERE r.reac IS NOT NULL
    RETURN r.reac AS reaction, count(*) AS cnt
    ORDER BY cnt DESC
    LIMIT $limit
    """
    rows = _neo4j_post(cypher, {"names": drug_names, "limit": limit})
    return [row[0] for row in rows if row[0]]


def _query_drug_indications(drug_names: list[str], limit: int = 5) -> list[str]:
    """Return top indications (usage contexts) for given drug names."""
    cypher = """
    MATCH (ds:DrugSet)-[:CONTAINS_DRUG]->(d:Drug)
    WHERE d.drugname IN $names
    WITH ds
    MATCH (ds)-[:TREATS_FOR]->(ind:Indication)
    WHERE ind.indi_pt IS NOT NULL
    RETURN ind.indi_pt AS indication, count(*) AS cnt
    ORDER BY cnt DESC
    LIMIT $limit
    """
    rows = _neo4j_post(cypher, {"names": drug_names, "limit": limit})
    return [row[0] for row in rows if row[0]]


def _extract_drug_names(question: str) -> list[str]:
    """Extract English canonical drug names mentioned (directly or via Chinese map)."""
    found: list[str] = []
    q_upper = question.upper()

    # Match English drug names directly
    for names in DRUG_NAME_MAP.values():
        for name in names:
            if name in q_upper and name not in found:
                found.append(name)

    # Match Chinese drug names
    for zh, en_names in DRUG_NAME_MAP.items():
        if zh in question:
            for name in en_names:
                if name not in found:
                    found.append(name)

    return found


def _build_graph_context(question: str) -> str | None:
    """Query Neo4j for drug facts relevant to the question, return as context string."""
    if not NEO4J_ENABLED:
        return None
    try:
        drug_names = _extract_drug_names(question)
        if not drug_names:
            return None

        t0 = time.time()
        reactions   = _query_drug_reactions(drug_names)
        indications = _query_drug_indications(drug_names)
        elapsed = int((time.time() - t0) * 1000)

        if not reactions and not indications:
            return None

        lines = [f"【来自药物不良事件数据库（FAERS）的结构化知识 — 查询耗时 {elapsed}ms】"]
        if reactions:
            lines.append(f"涉及药物：{', '.join(drug_names)}")
            lines.append(f"已报告不良反应（Top {len(reactions)}）：{', '.join(reactions)}")
        if indications:
            lines.append(f"常见用药适应症：{', '.join(indications)}")
        lines.append("（以上数据来源于真实世界药物警戒报告，供参考）")

        ctx = "\n".join(lines)
        logger.info("Neo4j augmentation: drugs=%s reactions=%d indications=%d elapsed=%dms",
                    drug_names, len(reactions), len(indications), elapsed)
        return ctx
    except Exception as e:
        logger.warning("Neo4j augmentation failed: %s", e)
        return None


# ── Redis predict cache ───────────────────────────────────────────────────────

def _predict_cache_key(text: str) -> str:
    return "ai:predict:v1:" + hashlib.sha256(text.encode()).hexdigest()


def _predict_cache_get(text: str) -> dict | None:
    r = _get_redis()
    if r is None:
        return None
    try:
        raw = r.get(_predict_cache_key(text))
        return json.loads(raw) if raw else None
    except Exception:
        return None


def _predict_cache_set(text: str, value: dict) -> None:
    r = _get_redis()
    if r is None:
        return
    try:
        # TTL jitter ±10% to avoid cache stampede
        ttl = PREDICT_CACHE_TTL + int(PREDICT_CACHE_TTL * 0.1 * (hash(text) % 3 - 1))
        r.setex(_predict_cache_key(text), ttl, json.dumps(value, ensure_ascii=False))
    except Exception:
        pass


# ── LLM helper ────────────────────────────────────────────────────────────────

def call_llm(messages: list[dict[str, Any]], model: str = RAG_MODEL, max_tokens: int = 1024) -> str:
    resp = Generation.call(
        model=model,
        messages=messages,
        max_tokens=max_tokens,
        result_format="message",
        temperature=0.1,
    )
    if resp.status_code != 200:
        raise RuntimeError("DashScope error %s: %s" % (resp.status_code, resp.message))
    return resp.output.choices[0].message.content.strip()


def parse_predictions(raw: str) -> list:
    try:
        parsed = json.loads(raw)
        if isinstance(parsed, list):
            return parsed
        for v in parsed.values():
            if isinstance(v, list):
                return v
    except (json.JSONDecodeError, AttributeError):
        pass
    m = re.search(r'\[.*?\]', raw, re.DOTALL)
    if m:
        try:
            return json.loads(m.group())
        except json.JSONDecodeError:
            pass
    return []


def normalize_predictions(raw_list: list) -> list:
    result = []
    for item in raw_list[:5]:
        if not isinstance(item, dict):
            continue
        reaction = (item.get("reaction") or item.get("name") or "").strip()
        prob = float(item.get("probability") or item.get("prob") or 0.5)
        if reaction:
            result.append({"reaction": reaction, "probability": round(prob, 2)})
    return result


# ── RAG ───────────────────────────────────────────────────────────────────────

@app.post("/rag/query")
async def rag_query(req: RagRequest):
    t0 = time.time()
    question = req.question.strip()

    if not question:
        return JSONResponse(
            status_code=400,
            content={
                "success": False,
                "error": "question cannot be empty",
                "error_code": "EMPTY_QUESTION",
                "provider_status": "error",
            },
        )
    try:
        # Graph augmentation: inject structured drug facts into LLM prompt
        graph_ctx = _build_graph_context(question)
        neo4j_elapsed = 0
        if graph_ctx:
            neo4j_elapsed = int((time.time() - t0) * 1000)

        if graph_ctx:
            user_content = (
                f"{question}\n\n"
                f"---\n{graph_ctx}\n---\n"
                "请结合以上来自药物不良事件数据库的结构化参考数据，给出更准确的回答。"
            )
        else:
            user_content = question

        llm_t0 = time.time()
        answer = call_llm([
            {"role": "system", "content": RAG_SYSTEM},
            {"role": "user",   "content": user_content},
        ])
        llm_elapsed = int((time.time() - llm_t0) * 1000)
        elapsed_ms = int((time.time() - t0) * 1000)

        result: dict[str, Any] = {
            "success": True,
            "answer": answer,
            "sources": [],
            "cache_hit": False,
            "elapsed_ms": elapsed_ms,
            "provider_status": "provider",
            "error_code": None,
            "neo4j_augmented": graph_ctx is not None,
        }
        if req.with_timing:
            result["timings"] = {
                "total_ms": elapsed_ms,
                "neo4j_ms": neo4j_elapsed,
                "llm_ms": llm_elapsed,
            }
        if req.with_trace:
            result["trace"] = {
                "question": question,
                "top_k": req.top_k,
                "strategy": req.strategy,
                "kb_version": req.knowledge_base_version,
                "model": RAG_MODEL,
                "graph_context": graph_ctx,
            }
        return result
    except Exception as exc:
        elapsed_ms = int((time.time() - t0) * 1000)
        logger.error("RAG error: %s", traceback.format_exc())
        return JSONResponse(
            status_code=502,
            content={
                "success": False,
                "answer": None,
                "error": str(exc),
                "error_code": "LLM_ERROR",
                "elapsed_ms": elapsed_ms,
                "provider_status": "error",
            },
        )


# ── Predict ───────────────────────────────────────────────────────────────────

@app.post("/api/predict/analyze")
async def predict_analyze(req: PredictRequest):
    text = req.text.strip()
    if not text:
        return JSONResponse(
            status_code=400,
            content={"status": "error", "message": "text cannot be empty"},
        )

    # Redis cache check
    if PREDICT_CACHE_ENABLED:
        cached = _predict_cache_get(text)
        if cached is not None:
            logger.info("Predict cache HIT for text len=%d", len(text))
            return {"status": "success", "predictions": cached, "cache_hit": True}

    try:
        raw = call_llm([
            {"role": "system", "content": PREDICT_SYSTEM},
            {"role": "user",   "content": "临床摘要：\n" + text},
        ], max_tokens=512)
        predictions = normalize_predictions(parse_predictions(raw))
        if not predictions:
            predictions = [{"reaction": "无法解析预测结果", "probability": 0.0}]

        if PREDICT_CACHE_ENABLED:
            _predict_cache_set(text, predictions)

        return {"status": "success", "predictions": predictions, "cache_hit": False}
    except Exception as exc:
        return JSONResponse(
            status_code=502,
            content={"status": "error", "message": str(exc)},
        )


# ── OCR ───────────────────────────────────────────────────────────────────────

@app.post("/ocr/predict")
async def ocr_predict(file: UploadFile = File(...)):
    img_bytes = await file.read()
    try:
        b64 = base64.b64encode(img_bytes).decode("utf-8")
        resp = MultiModalConversation.call(
            model=OCR_MODEL,
            messages=[{
                "role": "user",
                "content": [
                    {"image": "data:image/jpeg;base64," + b64},
                    {"text": "请识别图片中药品说明书的文字内容，以纯文本格式输出。"},
                ],
            }],
        )
        if resp.status_code != 200:
            raise RuntimeError("DashScope OCR error %s: %s" % (resp.status_code, resp.message))
        output = resp.output.choices[0].message.content[0]["text"].strip()
        return {"status": "success", "output": output, "ocr_result": {"text": output}}
    except Exception as exc:
        return JSONResponse(
            status_code=502,
            content={"status": "error", "message": "OCR error: " + str(exc), "output": ""},
        )


# ── Health ────────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    neo4j_ok = False
    redis_ok  = False
    try:
        _neo4j_post("RETURN 1")
        neo4j_ok = True
    except Exception:
        pass
    r = _get_redis()
    if r is not None:
        try:
            r.ping()
            redis_ok = True
        except Exception:
            pass
    return {
        "status": "ok",
        "service": "medical-fastapi-ai",
        "port": 8001,
        "model": RAG_MODEL,
        "neo4j": neo4j_ok,
        "redis": redis_ok,
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
