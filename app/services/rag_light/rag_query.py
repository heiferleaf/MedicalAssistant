import contextlib
import time
from io import StringIO
from typing import Any, Dict

from app.services.rag_light.core.input_layer import parse_input
from app.services.rag_light.core.initial_search_layer import initial_search
from app.services.rag_light.core.relation_aggregate_layer import aggregate_relations
from app.services.rag_light.core.ranking_layer import rank_expansions
from app.services.rag_light.core.output_layer import generate_answer


def rag_query(
    question: str,
    *,
    with_trace: bool = False,
    with_timing: bool = False,
    suppress_internal_logs: bool = True,
) -> Dict[str, Any]:
    timings: Dict[str, float] = {}
    trace: Dict[str, Any] = {}

    def _call(fn, *args, label: str):
        start = time.time()
        if suppress_internal_logs:
            with contextlib.redirect_stdout(StringIO()):
                out = fn(*args)
        else:
            out = fn(*args)
        cost = time.time() - start
        if with_timing:
            timings[label] = cost
        return out

    try:
        parsed = _call(parse_input, question, label="layer1_parse")
        if with_trace:
            trace["parsed"] = parsed

        searched = _call(initial_search, parsed, label="layer2_search")
        if with_trace:
            trace["searched"] = searched

        aggregated = _call(aggregate_relations, searched, label="layer3_aggregate")
        if with_trace:
            trace["aggregated"] = aggregated

        ranked = _call(rank_expansions, aggregated, label="layer4_rank")
        if with_trace:
            trace["ranked"] = ranked

        answer_dict = _call(generate_answer, ranked, label="layer5_generate")

        if with_timing:
            timings["total"] = sum(
                timings.get(k, 0.0)
                for k in (
                    "layer1_parse",
                    "layer2_search",
                    "layer3_aggregate",
                    "layer4_rank",
                    "layer5_generate",
                )
            )

        answer_text = (answer_dict or {}).get("answer", "") if isinstance(answer_dict, dict) else ""

        result: Dict[str, Any] = {
            "answer": answer_text,
            "success": True,
            "error": "",
        }
        if with_timing:
            result["timings"] = timings
        if with_trace:
            result["trace"] = trace
        result["raw"] = answer_dict
        return result

    except Exception as exc:  # noqa: BLE001
        err = repr(exc)
        result = {
            "answer": "",
            "success": False,
            "error": err,
        }
        if with_timing:
            result["timings"] = timings
        if with_trace:
            result["trace"] = trace
        return result
