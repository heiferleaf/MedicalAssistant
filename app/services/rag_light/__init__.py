from __future__ import annotations

from typing import Any, Dict


def rag_query(
	question: str,
	*,
	with_trace: bool = False,
	with_timing: bool = False,
	suppress_internal_logs: bool = True,
) -> Dict[str, Any]:
	# Lazy import to avoid triggering heavyweight imports (Neo4j/Ollama config)
	# at package import time. This keeps CLI tools predictable.
	from app.services.rag_light.rag_query import rag_query as _rag_query

	return _rag_query(
		question,
		with_trace=with_trace,
		with_timing=with_timing,
		suppress_internal_logs=suppress_internal_logs,
	)


__all__ = ["rag_query"]
