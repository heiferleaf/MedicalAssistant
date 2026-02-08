from typing import Dict, List

from app.services.rag_light.llm_client import chat
from app.services.rag_light.retriever import LightRetriever


class LightRagPipeline:
    def __init__(self, data_path: str | None = None) -> None:
        self.retriever = LightRetriever(data_path=data_path)

    def answer(self, question: str, topk: int = 8) -> Dict[str, object]:
        mode, rows = self.retriever.retrieve(question, topk=topk)
        context_lines: List[str] = []
        for idx, row in enumerate(rows, 1):
            drug = row.get("drugname", "")
            reac = row.get("reac", "")
            freq = row.get("freq", 0)
            context_lines.append(f"{idx}. {drug} -> {reac} (freq={freq})")

        if not context_lines:
            return {
                "answer": "未找到相关记录，请尝试更具体的药品名称。",
                "success": True,
                "error": "",
                "source": mode,
            }

        prompt = (
            "你是药物安全问答助手，请基于给定要点回答用户问题。\n"
            "要求：不编造，简洁专业，必要时使用分点。\n\n"
            f"用户问题：{question}\n\n"
            "要点：\n" + "\n".join(context_lines)
        )

        messages = [
            {"role": "system", "content": "你是医学药物安全知识助手。"},
            {"role": "user", "content": prompt},
        ]

        try:
            answer = chat(messages)
            return {
                "answer": answer,
                "success": True,
                "error": "",
                "source": mode,
            }
        except Exception as exc:  # noqa: BLE001
            fallback = "\n".join(context_lines)
            return {
                "answer": f"{fallback}",
                "success": True,
                "error": f"LLM调用失败，已回退要点列表：{exc}",
                "source": mode,
            }
