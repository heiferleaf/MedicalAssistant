from typing import Any, Dict

from app.services.rag_service import RagService


def rag_query_tool(*, question: str, with_trace: bool, with_timing: bool) -> Dict[str, Any]:
    service = RagService()
    return service.query(question=question, with_trace=with_trace, with_timing=with_timing)
