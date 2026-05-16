#!/usr/bin/env python3
"""
Benchmark script for MedicalAssistant AI / RAG / Predict endpoints.

It uses only Python standard library so it can run on a clean server:

  python3 test/ai-rag-benchmark/ai_rag_benchmark.py \
    --base-url http://127.0.0.1:8080 \
    --cases test/ai-rag-benchmark/ai_rag_benchmark_cases.sample.jsonl \
    --concurrency 4 \
    --repeat 2
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import os
import statistics
import sys
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from typing import Any, Dict, Iterable, List, Optional, Tuple


Json = Dict[str, Any]


def load_cases(path: str) -> List[Json]:
    cases: List[Json] = []
    with open(path, "r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            try:
                case = json.loads(line)
            except json.JSONDecodeError as exc:
                raise ValueError(f"{path}:{line_no} is not valid JSONL: {exc}") from exc
            case.setdefault("id", f"case-{line_no}")
            case.setdefault("endpoint", "rag")
            cases.append(case)
    return cases


def post_json(url: str, payload: Json, timeout: float) -> Tuple[int, Json, str]:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            return resp.status, json.loads(raw) if raw else {}, raw
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            parsed = {"raw": raw}
        return exc.code, parsed, raw


def normalize_base_url(base_url: str) -> str:
    return base_url.rstrip("/")


def endpoint_url(base_url: str, endpoint: str) -> str:
    mapping = {
        "rag": "/api/rag/query",
        "agent": "/api/agent/chat",
        "predict": "/api/predict/analyze",
    }
    if endpoint not in mapping:
        raise ValueError(f"unsupported endpoint={endpoint!r}; use rag, agent, or predict")
    return normalize_base_url(base_url) + mapping[endpoint]


def build_payload(case: Json, repeat_index: int) -> Json:
    endpoint = case.get("endpoint", "rag")
    payload = dict(case.get("payload") or {})

    if endpoint == "rag":
        payload.setdefault("question", case.get("question", case.get("input", "")))
        payload.setdefault("with_trace", case.get("with_trace", True))
        payload.setdefault("with_timing", case.get("with_timing", True))
    elif endpoint == "agent":
        payload.setdefault("user_id", str(case.get("user_id", "10001")))
        session_base = case.get("session_id", f"bench-{case.get('id', 'case')}")
        payload.setdefault("session_id", f"{session_base}-r{repeat_index}")
        payload.setdefault("message", case.get("message", case.get("question", case.get("input", ""))))
        payload.setdefault("with_trace", case.get("with_trace", True))
        payload.setdefault("with_timing", case.get("with_timing", True))
    elif endpoint == "predict":
        payload.setdefault("text", case.get("text", case.get("input", "")))
    else:
        raise ValueError(f"unsupported endpoint={endpoint!r}")

    return payload


def unwrap_result(endpoint: str, response_json: Json) -> Json:
    if endpoint in {"agent", "predict"} and isinstance(response_json.get("data"), dict):
        return response_json["data"]
    return response_json


def answer_text(endpoint: str, unwrapped: Json) -> str:
    if endpoint == "rag":
        return str(unwrapped.get("answer") or "")
    if endpoint == "agent":
        return str(unwrapped.get("assistant_message") or unwrapped.get("message") or "")
    if endpoint == "predict":
        predictions = unwrapped.get("predictions") or []
        if isinstance(predictions, list):
            return " ".join(str(p.get("reaction", "")) for p in predictions if isinstance(p, dict))
    return ""


def contains_all(text: str, keywords: Iterable[str]) -> Tuple[bool, List[str]]:
    missing = [kw for kw in keywords if kw and kw.lower() not in text.lower()]
    return not missing, missing


def predict_reaction_hit(unwrapped: Json, expected: List[str], top_k: int) -> Tuple[bool, List[str]]:
    predictions = unwrapped.get("predictions") or []
    if not isinstance(predictions, list):
        return False, expected
    reactions = []
    for item in predictions[:top_k]:
        if isinstance(item, dict):
            reactions.append(str(item.get("reaction", "")))
    joined = " ".join(reactions).lower()
    missing = [name for name in expected if name.lower() not in joined]
    return not missing, missing


def score_case(case: Json, status_code: int, response_json: Json, raw: str) -> Json:
    endpoint = case.get("endpoint", "rag")
    unwrapped = unwrap_result(endpoint, response_json)
    text = answer_text(endpoint, unwrapped)

    ok_status = 200 <= status_code < 300
    app_code = response_json.get("code")
    if app_code is not None:
        ok_status = ok_status and int(app_code) == int(case.get("expected_code", 200))

    service_success = True
    if endpoint == "rag" and "success" in unwrapped:
        service_success = bool(unwrapped.get("success"))
    if endpoint == "predict" and "status" in unwrapped:
        service_success = str(unwrapped.get("status")).lower() == str(case.get("expected_status", "success")).lower()
    if endpoint == "agent" and "success" in unwrapped:
        service_success = bool(unwrapped.get("success"))

    expected_keywords = list(case.get("expected_keywords") or [])
    keyword_ok, missing_keywords = contains_all(text, expected_keywords)

    forbidden_keywords = list(case.get("forbidden_keywords") or [])
    forbidden_ok = not any(kw and kw.lower() in text.lower() for kw in forbidden_keywords)

    reaction_ok = True
    missing_reactions: List[str] = []
    expected_reactions = list(case.get("expected_reactions") or [])
    if endpoint == "predict" and expected_reactions:
        reaction_ok, missing_reactions = predict_reaction_hit(
            unwrapped,
            expected_reactions,
            int(case.get("top_k", 5)),
        )

    min_probability_ok = True
    if endpoint == "predict" and case.get("min_top_probability") is not None:
        predictions = unwrapped.get("predictions") or []
        top_probability = None
        if predictions and isinstance(predictions[0], dict):
            top_probability = predictions[0].get("probability")
        min_probability_ok = top_probability is not None and float(top_probability) >= float(case["min_top_probability"])

    passed = ok_status and service_success and keyword_ok and forbidden_ok and reaction_ok and min_probability_ok
    return {
        "passed": passed,
        "ok_status": ok_status,
        "service_success": service_success,
        "keyword_ok": keyword_ok,
        "forbidden_ok": forbidden_ok,
        "reaction_ok": reaction_ok,
        "min_probability_ok": min_probability_ok,
        "missing_keywords": missing_keywords,
        "missing_reactions": missing_reactions,
        "answer_chars": len(text),
        "answer_preview": text[:240].replace("\n", " "),
        "raw_preview": raw[:240].replace("\n", " "),
    }


def run_one(args: argparse.Namespace, case: Json, repeat_index: int) -> Json:
    endpoint = case.get("endpoint", "rag")
    url = endpoint_url(args.base_url, endpoint)
    payload = build_payload(case, repeat_index)
    started = time.perf_counter()
    try:
        status_code, response_json, raw = post_json(url, payload, args.timeout)
        error = ""
    except Exception as exc:
        status_code, response_json, raw = 0, {}, ""
        error = f"{type(exc).__name__}: {exc}"
        if args.debug:
            error += "\n" + traceback.format_exc()
    elapsed_ms = (time.perf_counter() - started) * 1000.0

    score = score_case(case, status_code, response_json, raw) if not error else {
        "passed": False,
        "ok_status": False,
        "service_success": False,
        "keyword_ok": False,
        "forbidden_ok": False,
        "reaction_ok": False,
        "min_probability_ok": False,
        "missing_keywords": case.get("expected_keywords") or [],
        "missing_reactions": case.get("expected_reactions") or [],
        "answer_chars": 0,
        "answer_preview": "",
        "raw_preview": "",
    }

    return {
        "case_id": case.get("id"),
        "endpoint": endpoint,
        "repeat_index": repeat_index,
        "status_code": status_code,
        "elapsed_ms": round(elapsed_ms, 3),
        "error": error,
        **score,
    }


def percentile(values: List[float], pct: float) -> Optional[float]:
    if not values:
        return None
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    pos = (len(ordered) - 1) * pct
    lower = math.floor(pos)
    upper = math.ceil(pos)
    if lower == upper:
        return ordered[int(pos)]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (pos - lower)


def summarize(results: List[Json], started_at: float, finished_at: float) -> Json:
    elapsed_values = [float(r["elapsed_ms"]) for r in results if not r.get("error")]
    total = len(results)
    passed = sum(1 for r in results if r.get("passed"))
    errors = sum(1 for r in results if r.get("error") or not r.get("ok_status"))
    by_endpoint: Dict[str, Json] = {}

    for endpoint in sorted({r["endpoint"] for r in results}):
        subset = [r for r in results if r["endpoint"] == endpoint]
        vals = [float(r["elapsed_ms"]) for r in subset if not r.get("error")]
        by_endpoint[endpoint] = {
            "total": len(subset),
            "passed": sum(1 for r in subset if r.get("passed")),
            "accuracy": round(sum(1 for r in subset if r.get("passed")) / len(subset), 4) if subset else 0,
            "error_rate": round(sum(1 for r in subset if r.get("error") or not r.get("ok_status")) / len(subset), 4) if subset else 0,
            "avg_ms": round(statistics.mean(vals), 3) if vals else None,
            "p50_ms": round(percentile(vals, 0.50), 3) if vals else None,
            "p90_ms": round(percentile(vals, 0.90), 3) if vals else None,
            "p95_ms": round(percentile(vals, 0.95), 3) if vals else None,
            "p99_ms": round(percentile(vals, 0.99), 3) if vals else None,
        }

    duration = max(finished_at - started_at, 0.001)
    return {
        "total": total,
        "passed": passed,
        "accuracy": round(passed / total, 4) if total else 0,
        "error_rate": round(errors / total, 4) if total else 0,
        "duration_sec": round(duration, 3),
        "throughput_rps": round(total / duration, 3),
        "avg_ms": round(statistics.mean(elapsed_values), 3) if elapsed_values else None,
        "p50_ms": round(percentile(elapsed_values, 0.50), 3) if elapsed_values else None,
        "p90_ms": round(percentile(elapsed_values, 0.90), 3) if elapsed_values else None,
        "p95_ms": round(percentile(elapsed_values, 0.95), 3) if elapsed_values else None,
        "p99_ms": round(percentile(elapsed_values, 0.99), 3) if elapsed_values else None,
        "by_endpoint": by_endpoint,
    }


def write_outputs(output_dir: str, results: List[Json], summary: Json) -> Tuple[str, str]:
    os.makedirs(output_dir, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    json_path = os.path.join(output_dir, f"ai_rag_benchmark_{stamp}.json")
    csv_path = os.path.join(output_dir, f"ai_rag_benchmark_{stamp}.csv")

    with open(json_path, "w", encoding="utf-8") as f:
        json.dump({"summary": summary, "results": results}, f, ensure_ascii=False, indent=2)

    fieldnames = [
        "case_id", "endpoint", "repeat_index", "passed", "status_code", "elapsed_ms",
        "error", "service_success", "keyword_ok", "reaction_ok", "missing_keywords",
        "missing_reactions", "answer_chars", "answer_preview",
    ]
    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in results:
            writer.writerow({key: json.dumps(row.get(key), ensure_ascii=False) if isinstance(row.get(key), (list, dict)) else row.get(key) for key in fieldnames})

    return json_path, csv_path


def main() -> int:
    parser = argparse.ArgumentParser(description="MedicalAssistant AI/RAG benchmark runner")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080", help="Spring Boot base URL")
    parser.add_argument("--cases", default="test/ai-rag-benchmark/ai_rag_benchmark_cases.sample.jsonl", help="JSONL benchmark cases")
    parser.add_argument("--concurrency", type=int, default=1, help="Concurrent requests")
    parser.add_argument("--repeat", type=int, default=1, help="Repeat each case N times")
    parser.add_argument("--timeout", type=float, default=180.0, help="HTTP timeout seconds per request")
    parser.add_argument("--warmup", type=int, default=0, help="Warmup requests per case, excluded from metrics")
    parser.add_argument("--output-dir", default="test/ai-rag-benchmark/benchmark-results", help="Where to write JSON/CSV results")
    parser.add_argument("--debug", action="store_true", help="Include traceback in per-case error")
    args = parser.parse_args()

    cases = load_cases(args.cases)
    if not cases:
        print(f"No cases found in {args.cases}", file=sys.stderr)
        return 2

    if args.warmup > 0:
        print(f"Warmup: {len(cases) * args.warmup} requests")
        for case in cases:
            for i in range(args.warmup):
                run_one(args, case, i)

    jobs = [(case, repeat_index) for repeat_index in range(args.repeat) for case in cases]
    print(f"Running {len(jobs)} requests against {args.base_url} with concurrency={args.concurrency}")

    started_at = time.perf_counter()
    results: List[Json] = []
    with ThreadPoolExecutor(max_workers=max(args.concurrency, 1)) as pool:
        futures = [pool.submit(run_one, args, case, repeat_index) for case, repeat_index in jobs]
        for future in as_completed(futures):
            row = future.result()
            results.append(row)
            mark = "PASS" if row["passed"] else "FAIL"
            print(f"{mark} {row['endpoint']} {row['case_id']} r{row['repeat_index']} {row['elapsed_ms']}ms")
    finished_at = time.perf_counter()

    results.sort(key=lambda r: (str(r["endpoint"]), str(r["case_id"]), int(r["repeat_index"])))
    summary = summarize(results, started_at, finished_at)
    json_path, csv_path = write_outputs(args.output_dir, results, summary)

    print("\nSummary")
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    print(f"\nWrote:\n  {json_path}\n  {csv_path}")
    return 0 if summary["error_rate"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
