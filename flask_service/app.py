"""Medical AI FastAPI Service — RAG / Predict / OCR via DashScope (Qwen)."""
import base64
import json
import os
import re
import time
import traceback
from typing import Any, Optional

import dashscope
from dashscope import Generation, MultiModalConversation
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel

app = FastAPI(title="Medical AI Service", version="1.0.0")

dashscope.api_key = os.environ.get("DASHSCOPE_API_KEY", "sk-e7051a4ddaa049e9bd25c8264dfb3b15")

RAG_MODEL = "qwen-plus"
OCR_MODEL = "qwen-vl-plus"

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


# ── Helpers ───────────────────────────────────────────────────────────────────

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
        answer = call_llm([
            {"role": "system", "content": RAG_SYSTEM},
            {"role": "user",   "content": question},
        ])
        elapsed_ms = int((time.time() - t0) * 1000)
        result: dict[str, Any] = {
            "success": True,
            "answer": answer,
            "sources": [],
            "cache_hit": False,
            "elapsed_ms": elapsed_ms,
            "provider_status": "provider",
            "error_code": None,
        }
        if req.with_timing:
            result["timings"] = {"total_ms": elapsed_ms, "llm_ms": elapsed_ms}
        if req.with_trace:
            result["trace"] = {
                "question": question,
                "top_k": req.top_k,
                "strategy": req.strategy,
                "kb_version": req.knowledge_base_version,
                "model": RAG_MODEL,
            }
        return result
    except Exception as exc:
        elapsed_ms = int((time.time() - t0) * 1000)
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
    try:
        raw = call_llm([
            {"role": "system", "content": PREDICT_SYSTEM},
            {"role": "user",   "content": "临床摘要：\n" + text},
        ], max_tokens=512)
        predictions = normalize_predictions(parse_predictions(raw))
        if not predictions:
            predictions = [{"reaction": "无法解析预测结果", "probability": 0.0}]
        return {"status": "success", "predictions": predictions}
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
    return {"status": "ok", "service": "medical-fastapi-ai", "port": 8001}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
