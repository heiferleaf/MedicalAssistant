import json
import os
from typing import Dict, List, Tuple

DEFAULT_SAMPLE_PATH = os.path.join(
    os.path.dirname(__file__), "data", "drug_reaction_stats_sample.json"
)


def _load_data(path: str) -> List[Dict[str, object]]:
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def _normalize(text: str) -> str:
    return (text or "").strip().lower()


class LightRetriever:
    def __init__(self, data_path: str | None = None) -> None:
        self.data_path = data_path or DEFAULT_SAMPLE_PATH
        self._rows = _load_data(self.data_path)

    def retrieve(self, question: str, topk: int = 8) -> Tuple[str, List[Dict[str, object]]]:
        q = _normalize(question)
        if not q:
            return "", []

        # 简单匹配 drugname 或 reac 关键词
        matched = []
        for row in self._rows:
            drug = _normalize(str(row.get("drugname", "")))
            reac = _normalize(str(row.get("reac", "")))
            score = 0
            if drug and drug in q:
                score += 2
            if reac and reac in q:
                score += 1
            if score > 0:
                matched.append((score, row))

        if not matched:
            # 无明确命中时，回退为高频条目
            sorted_rows = sorted(self._rows, key=lambda r: r.get("freq", 0), reverse=True)
            return "fallback_top", sorted_rows[:topk]

        matched.sort(key=lambda x: (x[0], x[1].get("freq", 0)), reverse=True)
        result = [m[1] for m in matched][:topk]
        return "keyword_match", result
