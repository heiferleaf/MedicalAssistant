from collections import defaultdict
from typing import Any, Dict, List

from py2neo import Graph
from py2neo.errors import ClientError

from app.services.rag_light.core.input_layer import parse_input

NEO4J_URI = "bolt://localhost:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = ""

NEO4J_URI = __import__("os").getenv("NEO4J_URI", NEO4J_URI)
NEO4J_USER = __import__("os").getenv("NEO4J_USER", NEO4J_USER)
NEO4J_PASSWORD = __import__("os").getenv("NEO4J_PASSWORD", NEO4J_PASSWORD)

graph = Graph(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))

LABEL_INFO: Dict[str, Dict[str, str]] = {
    "Drug": {"prop": "drugname", "fulltext": "drug_ft"},
    "Reaction": {"prop": "reac", "fulltext": "reaction_ft"},
    "Indication": {"prop": "indi", "fulltext": "indication_ft"},
    "Outcome": {"prop": "outccode", "fulltext": "outcome_ft"},
}


def _fulltext_query(label: str, term: str, limit: int) -> List[Dict[str, Any]]:
    info = LABEL_INFO[label]
    prop = info["prop"]
    index_name = info["fulltext"]

    query = f"""
    CALL db.index.fulltext.queryNodes($index, $term) YIELD node, score
    RETURN id(node) AS id, node.{prop} AS value, node.embedding AS embedding, score
    ORDER BY score DESC
    LIMIT $limit
    """
    return graph.run(query, index=index_name, term=term, limit=limit).data()


def _contains_query(label: str, term: str, limit: int) -> List[Dict[str, Any]]:
    info = LABEL_INFO[label]
    prop = info["prop"]
    query = f"""
    MATCH (n:{label})
    WHERE toLower(n.{prop}) CONTAINS toLower($term)
    RETURN id(n) AS id, n.{prop} AS value, n.embedding AS embedding
    ORDER BY toLower(n.{prop}) ASC
    LIMIT $limit
    """
    rows = graph.run(query, term=term, limit=limit).data()
    for r in rows:
        r["score"] = None
    return rows


def _fallback_all(label: str, limit: int) -> List[Dict[str, Any]]:
    info = LABEL_INFO[label]
    prop = info["prop"]
    query = f"""
    MATCH (n:{label})
    RETURN id(n) AS id, n.{prop} AS value, n.embedding AS embedding
    ORDER BY toLower(n.{prop}) ASC
    LIMIT $limit
    """
    rows = graph.run(query, limit=limit).data()
    for r in rows:
        r["score"] = None
    return rows


def _search_single_seed(label: str, term: str, limit: int) -> List[Dict[str, Any]]:
    if not term:
        return []
    try:
        rows = _fulltext_query(label, term, limit)
        if rows:
            return rows
    except ClientError:
        pass
    except Exception:
        pass
    return _contains_query(label, term, limit)


def initial_search(parsed: Dict[str, Any], per_seed_limit: int = 40, fallback_limit: int = 20) -> Dict[str, Any]:
    seeds = parsed.get("seeds", [])
    intents = parsed.get("intents", [])
    topk = parsed.get("topk", 10)

    candidates: Dict[str, Dict[int, Dict[str, Any]]] = defaultdict(dict)

    for seed in seeds:
        label = seed["type"]
        if label not in LABEL_INFO:
            continue
        term = seed.get("normalized") or seed.get("text")
        rows = _search_single_seed(label, term, per_seed_limit)
        for r in rows:
            nid = int(r["id"])
            existing = candidates[label].get(nid)
            if not existing or (r.get("score") or 0) > (existing.get("score") or 0):
                candidates[label][nid] = {
                    "id": nid,
                    "label": label,
                    "value": r.get("value"),
                    "score": r.get("score"),
                    "embedding": r.get("embedding"),
                    "seed": term,
                }

    for label in intents:
        if label not in LABEL_INFO:
            continue
        if not candidates[label]:
            rows = _fallback_all(label, fallback_limit)
            for r in rows:
                nid = int(r["id"])
                candidates[label][nid] = {
                    "id": nid,
                    "label": label,
                    "value": r.get("value"),
                    "score": r.get("score"),
                    "embedding": r.get("embedding"),
                    "seed": None,
                }

    result_items: Dict[str, List[Dict[str, Any]]] = {}
    for label, table in candidates.items():
        items = list(table.values())
        items.sort(key=lambda x: (x["score"] is not None, x["score"], x["value"]), reverse=True)
        result_items[label] = items[: max(topk, 10)]

    return {
        "question": parsed["question"],
        "seeds": parsed["seeds"],
        "intents": intents,
        "topk": topk,
        "query_vector": parsed.get("query_vector"),
        "candidates": result_items,
        "meta": {
            "input_meta": parsed.get("meta"),
            "per_seed_limit": per_seed_limit,
            "fallback_limit": fallback_limit,
        },
    }


if __name__ == "__main__":
    questions = [
        "阿司匹林的常见不良反应有哪些？",
        "NAUSEA 常见于哪些药物？",
    ]
    for q in questions:
        parsed = parse_input(q)
        initial_search(parsed)
