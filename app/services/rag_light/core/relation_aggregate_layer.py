import os
from functools import lru_cache
from typing import Any, Dict, List, Set, Tuple

from py2neo import Graph


# 用于线上排查：确认服务是否加载了最新的聚合查询逻辑。
RELATION_AGG_VERSION = "v3_group_by_text_normalized_2026-02-18"


def _neo4j_settings() -> Tuple[str, str, str]:
    uri = os.getenv("NEO4J_URI", "bolt://localhost:7687")
    user = os.getenv("NEO4J_USER", "neo4j")
    password = os.getenv("NEO4J_PASSWORD", "")
    return uri, user, password


@lru_cache(maxsize=1)
def _get_graph() -> Graph:
    uri, user, password = _neo4j_settings()
    return Graph(uri, auth=(user, password))

LABEL_PROP = {
    "Drug": "drugname",
    "Reaction": "reac",
    "Indication": "indi",
    "Outcome": "outccode",
}


def _candidate_ids(candidates: Dict[str, List[Dict[str, Any]]], label: str) -> List[int]:
    rows = candidates.get(label) or []
    return [int(r["id"]) for r in rows]


def _all_ids_exact(label: str, value: str) -> List[int]:
    if not value:
        return []
    prop = LABEL_PROP[label]
    q = f"""
    MATCH (n:{label})
    WHERE toLower(n.{prop}) = toLower($val)
    RETURN id(n) AS id
    """
    return [int(r["id"]) for r in _get_graph().run(q, val=value).data()]


def _distinct_drugset_count_by_drug_ids(drug_ids: List[int]) -> int:
    if not drug_ids:
        return 0
    q = """
    UNWIND $ids AS did
    MATCH (ds:DrugSet)-[:CONTAINS_DRUG]->(d:Drug) WHERE id(d)=did
    RETURN count(distinct ds) AS c
    """
    rec = _get_graph().run(q, ids=drug_ids).evaluate()
    return int(rec or 0)


def _distinct_drugset_count_by_other_ids(label: str, ids: List[int]) -> int:
    if not ids:
        return 0
    if label == "Reaction":
        q = """
        UNWIND $ids AS rid
        MATCH (ds:DrugSet)-[:CAUSES_REACTION]->(r:Reaction) WHERE id(r)=rid
        RETURN count(distinct ds) AS c
        """
    elif label == "Indication":
        q = """
        UNWIND $ids AS iid
        MATCH (ds:DrugSet)-[:TREATS_FOR]->(i:Indication) WHERE id(i)=iid
        RETURN count(distinct ds) AS c
        """
    elif label == "Outcome":
        q = """
        UNWIND $ids AS oid
        MATCH (p:Patient)-[:HAS_OUTCOME]->(o:Outcome) WHERE id(o)=oid
        MATCH (p)-[:USED_IN_CASE]->(ds:DrugSet)
        RETURN count(distinct ds) AS c
        """
    else:
        return 0
    rec = _get_graph().run(q, ids=ids).evaluate()
    return int(rec or 0)


def _reactions_from_drugs(drug_ids: List[int], k: int):
    if not drug_ids:
        return []
    q = """
    UNWIND $drugIds AS did
    MATCH (ds:DrugSet)-[:CONTAINS_DRUG]->(d:Drug) WHERE id(d)=did
    WITH distinct ds
    MATCH (ds)-[:CAUSES_REACTION]->(r:Reaction)
    WHERE r.reac IS NOT NULL AND trim(toString(r.reac)) <> ''
    WITH toLower(trim(toString(r.reac))) AS key,
         trim(toString(r.reac)) AS text_raw,
         min(id(r)) AS id,
         head(collect(r.embedding)) AS embedding,
         count(distinct ds) AS freq
    WHERE key <> 'pt'
    WITH key, id, embedding, freq, min(text_raw) AS text
    RETURN id AS id, 'Reaction' AS label, text, embedding, freq
    ORDER BY freq DESC
    LIMIT $k
    """
    return _get_graph().run(q, drugIds=drug_ids, k=k).data()


def _drugs_from_reactions(reaction_ids: List[int], k: int):
    if not reaction_ids:
        return []
    q = """
    UNWIND $reactionIds AS rid
    MATCH (ds:DrugSet)-[:CAUSES_REACTION]->(r:Reaction) WHERE id(r)=rid
    WITH distinct ds
    MATCH (ds)-[:CONTAINS_DRUG]->(d:Drug)
    WHERE d.drugname IS NOT NULL AND trim(toString(d.drugname)) <> ''
    WITH toUpper(trim(toString(d.drugname))) AS key,
         trim(toString(d.drugname)) AS text_raw,
         min(id(d)) AS id,
         head(collect(d.embedding)) AS embedding,
         count(distinct ds) AS freq
    WITH key, id, embedding, freq, min(text_raw) AS text
    RETURN id AS id, 'Drug' AS label, text, embedding, freq
    ORDER BY freq DESC
    LIMIT $k
    """
    return _get_graph().run(q, reactionIds=reaction_ids, k=k).data()


def _indications_from_drugs(drug_ids: List[int], k: int):
    if not drug_ids:
        return []
    q = """
    UNWIND $drugIds AS did
    MATCH (ds:DrugSet)-[:CONTAINS_DRUG]->(d:Drug) WHERE id(d)=did
    WITH distinct ds
    MATCH (ds)-[:TREATS_FOR]->(i:Indication)
    WHERE i.indi IS NOT NULL AND trim(toString(i.indi)) <> ''
        WITH toLower(trim(toString(i.indi))) AS key,
            trim(toString(i.indi)) AS text_raw,
         min(id(i)) AS id,
         head(collect(i.embedding)) AS embedding,
         count(distinct ds) AS freq
        WITH key, id, embedding, freq, min(text_raw) AS text
        RETURN id AS id, 'Indication' AS label, text, embedding, freq
    ORDER BY freq DESC
    LIMIT $k
    """
    return _get_graph().run(q, drugIds=drug_ids, k=k).data()


def _drugs_from_indications(indication_ids: List[int], k: int):
    if not indication_ids:
        return []
    q = """
    UNWIND $indicationIds AS iid
    MATCH (ds:DrugSet)-[:TREATS_FOR]->(i:Indication) WHERE id(i)=iid
    WITH distinct ds
    MATCH (ds)-[:CONTAINS_DRUG]->(d:Drug)
    WHERE d.drugname IS NOT NULL AND trim(toString(d.drugname)) <> ''
        WITH toUpper(trim(toString(d.drugname))) AS key,
            trim(toString(d.drugname)) AS text_raw,
         min(id(d)) AS id,
         head(collect(d.embedding)) AS embedding,
         count(distinct ds) AS freq
        WITH key, id, embedding, freq, min(text_raw) AS text
        RETURN id AS id, 'Drug' AS label, text, embedding, freq
    ORDER BY freq DESC
    LIMIT $k
    """
    return _get_graph().run(q, indicationIds=indication_ids, k=k).data()


def _outcomes_from_drugs(drug_ids: List[int], k: int):
    if not drug_ids:
        return []
    q = """
    UNWIND $drugIds AS did
    MATCH (p:Patient)-[:USED_IN_CASE]->(ds:DrugSet)-[:CONTAINS_DRUG]->(d:Drug) WHERE id(d)=did
    WITH distinct p
    MATCH (p)-[:HAS_OUTCOME]->(o:Outcome)
    WHERE o.outccode IS NOT NULL AND trim(toString(o.outccode)) <> ''
        WITH toUpper(trim(toString(o.outccode))) AS key,
            trim(toString(o.outccode)) AS text_raw,
         min(id(o)) AS id,
         head(collect(o.embedding)) AS embedding,
         count(distinct p) AS freq
        WITH key, id, embedding, freq, min(text_raw) AS text
        RETURN id AS id, 'Outcome' AS label, text, embedding, freq
    ORDER BY freq DESC
    LIMIT $k
    """
    return _get_graph().run(q, drugIds=drug_ids, k=k).data()


def _drugs_from_outcomes(outcome_ids: List[int], k: int):
    if not outcome_ids:
        return []
    q = """
    UNWIND $outcomeIds AS oid
    MATCH (p:Patient)-[:HAS_OUTCOME]->(o:Outcome) WHERE id(o)=oid
    WITH distinct p
    MATCH (p)-[:USED_IN_CASE]->(ds:DrugSet)
    MATCH (ds)-[:CONTAINS_DRUG]->(d:Drug)
    WHERE d.drugname IS NOT NULL AND trim(toString(d.drugname)) <> ''
        WITH toUpper(trim(toString(d.drugname))) AS key,
            trim(toString(d.drugname)) AS text_raw,
         min(id(d)) AS id,
         head(collect(d.embedding)) AS embedding,
         count(distinct p) AS freq
        WITH key, id, embedding, freq, min(text_raw) AS text
        RETURN id AS id, 'Drug' AS label, text, embedding, freq
    ORDER BY freq DESC
    LIMIT $k
    """
    return _get_graph().run(q, outcomeIds=outcome_ids, k=k).data()


def _global_top(label: str, k: int):
    if label == "Reaction":
        q = """
        MATCH (ds:DrugSet)-[:CAUSES_REACTION]->(r:Reaction)
       WHERE r.reac IS NOT NULL AND trim(toString(r.reac)) <> ''
       WITH toLower(trim(toString(r.reac))) AS key,
           trim(toString(r.reac)) AS text_raw,
             min(id(r)) AS id,
             head(collect(r.embedding)) AS embedding,
             count(distinct ds) AS freq
       WHERE key <> 'pt'
       WITH key, id, embedding, freq, min(text_raw) AS text
       RETURN id AS id, 'Reaction' AS label, text, embedding, freq
        ORDER BY freq DESC
        LIMIT $k
        """
    elif label == "Drug":
        q = """
        MATCH (ds:DrugSet)-[:CONTAINS_DRUG]->(d:Drug)
        WHERE d.drugname IS NOT NULL AND trim(toString(d.drugname)) <> ''
       WITH toUpper(trim(toString(d.drugname))) AS key,
           trim(toString(d.drugname)) AS text_raw,
             min(id(d)) AS id,
             head(collect(d.embedding)) AS embedding,
             count(distinct ds) AS freq
       WITH key, id, embedding, freq, min(text_raw) AS text
       RETURN id AS id, 'Drug' AS label, text, embedding, freq
        ORDER BY freq DESC
        LIMIT $k
        """
    elif label == "Indication":
        q = """
        MATCH (ds:DrugSet)-[:TREATS_FOR]->(i:Indication)
        WHERE i.indi IS NOT NULL AND trim(toString(i.indi)) <> ''
       WITH toLower(trim(toString(i.indi))) AS key,
           trim(toString(i.indi)) AS text_raw,
             min(id(i)) AS id,
             head(collect(i.embedding)) AS embedding,
             count(distinct ds) AS freq
       WITH key, id, embedding, freq, min(text_raw) AS text
       RETURN id AS id, 'Indication' AS label, text, embedding, freq
        ORDER BY freq DESC
        LIMIT $k
        """
    elif label == "Outcome":
        q = """
        MATCH (p:Patient)-[:HAS_OUTCOME]->(o:Outcome)
        WHERE o.outccode IS NOT NULL AND trim(toString(o.outccode)) <> ''
       WITH toUpper(trim(toString(o.outccode))) AS key,
           trim(toString(o.outccode)) AS text_raw,
             min(id(o)) AS id,
             head(collect(o.embedding)) AS embedding,
             count(distinct p) AS freq
       WITH key, id, embedding, freq, min(text_raw) AS text
       RETURN id AS id, 'Outcome' AS label, text, embedding, freq
        ORDER BY freq DESC
        LIMIT $k
        """
    else:
        return []
    return _get_graph().run(q, k=k).data()


def aggregate_relations(search_output: Dict[str, Any]) -> Dict[str, Any]:
    intents = search_output.get("intents", [])
    candidates = search_output.get("candidates", {})
    topk = search_output.get("topk", 10)
    seeds = search_output.get("seeds", [])

    seed_values_by_type: Dict[str, Set[str]] = {
        "Drug": set(),
        "Reaction": set(),
        "Indication": set(),
        "Outcome": set(),
    }
    for s in seeds:
        typ = s.get("type")
        norm = s.get("normalized")
        if typ in seed_values_by_type and norm:
            if typ == "Drug":
                norm = norm.upper()
            seed_values_by_type[typ].add(norm)

    for label, rows in candidates.items():
        if label not in seed_values_by_type:
            continue
        for r in rows:
            val = r.get("value")
            if val:
                if label == "Drug":
                    val = val.upper()
                seed_values_by_type[label].add(val)

    all_drug_ids: Set[int] = set()
    for name in seed_values_by_type["Drug"]:
        all_drug_ids.update(_all_ids_exact("Drug", name))
    all_reaction_ids: Set[int] = set()
    for name in seed_values_by_type["Reaction"]:
        all_reaction_ids.update(_all_ids_exact("Reaction", name))
    all_indication_ids: Set[int] = set()
    for name in seed_values_by_type["Indication"]:
        all_indication_ids.update(_all_ids_exact("Indication", name))
    all_outcome_ids: Set[int] = set()
    for name in seed_values_by_type["Outcome"]:
        all_outcome_ids.update(_all_ids_exact("Outcome", name))

    if not all_drug_ids and candidates.get("Drug"):
        all_drug_ids.update(_candidate_ids(candidates, "Drug"))
    if not all_reaction_ids and candidates.get("Reaction"):
        all_reaction_ids.update(_candidate_ids(candidates, "Reaction"))
    if not all_indication_ids and candidates.get("Indication"):
        all_indication_ids.update(_candidate_ids(candidates, "Indication"))
    if not all_outcome_ids and candidates.get("Outcome"):
        all_outcome_ids.update(_candidate_ids(candidates, "Outcome"))

    drug_ids = list(all_drug_ids)
    reaction_ids = list(all_reaction_ids)
    indication_ids = list(all_indication_ids)
    outcome_ids = list(all_outcome_ids)

    expansions: Dict[str, Dict[str, Any]] = {}
    for intent in intents:
        if intent == "Reaction":
            if drug_ids:
                rows = _reactions_from_drugs(drug_ids, topk)
                source = "Drug(seeds_full)"
                stats_count = _distinct_drugset_count_by_drug_ids(drug_ids)
                seed_names = list(seed_values_by_type["Drug"])
            else:
                rows = _global_top("Reaction", topk)
                source = "Global"
                stats_count = 0
                seed_names = []
            missing = sum(1 for r in rows if r.get("embedding") is None)
            expansions[intent] = {
                "source": source,
                "rows": rows,
                "columns": ["id", "label", "text", "embedding", "freq"],
                "stats": {
                    "distinct_drugset_count": stats_count,
                    "seed_drugs": seed_names,
                    "embedding_missing": missing,
                },
            }
        elif intent == "Drug":
            if reaction_ids:
                rows = _drugs_from_reactions(reaction_ids, topk)
                source = "Reaction(seeds_full)"
                stats_count = _distinct_drugset_count_by_other_ids("Reaction", reaction_ids)
                seed_names = list(seed_values_by_type["Reaction"])
            elif indication_ids:
                rows = _drugs_from_indications(indication_ids, topk)
                source = "Indication(seeds_full)"
                stats_count = _distinct_drugset_count_by_other_ids("Indication", indication_ids)
                seed_names = list(seed_values_by_type["Indication"])
            elif outcome_ids:
                rows = _drugs_from_outcomes(outcome_ids, topk)
                source = "Outcome(seeds_full)"
                stats_count = _distinct_drugset_count_by_other_ids("Outcome", outcome_ids)
                seed_names = list(seed_values_by_type["Outcome"])
            else:
                rows = _global_top("Drug", topk)
                source = "Global"
                stats_count = 0
                seed_names = []
            missing = sum(1 for r in rows if r.get("embedding") is None)
            expansions[intent] = {
                "source": source,
                "rows": rows,
                "columns": ["id", "label", "text", "embedding", "freq"],
                "stats": {
                    "distinct_drugset_count": stats_count,
                    "seed_terms": seed_names,
                    "embedding_missing": missing,
                },
            }
        elif intent == "Indication":
            if drug_ids:
                rows = _indications_from_drugs(drug_ids, topk)
                source = "Drug(seeds_full)"
                stats_count = _distinct_drugset_count_by_drug_ids(drug_ids)
                seed_names = list(seed_values_by_type["Drug"])
            else:
                rows = _global_top("Indication", topk)
                source = "Global"
                stats_count = 0
                seed_names = []
            missing = sum(1 for r in rows if r.get("embedding") is None)
            expansions[intent] = {
                "source": source,
                "rows": rows,
                "columns": ["id", "label", "text", "embedding", "freq"],
                "stats": {
                    "distinct_drugset_count": stats_count,
                    "seed_drugs": seed_names,
                    "embedding_missing": missing,
                },
            }
        elif intent == "Outcome":
            if drug_ids:
                rows = _outcomes_from_drugs(drug_ids, topk)
                source = "Drug(seeds_full)"
                stats_count = _distinct_drugset_count_by_drug_ids(drug_ids)
                seed_names = list(seed_values_by_type["Drug"])
            else:
                rows = _global_top("Outcome", topk)
                source = "Global"
                stats_count = 0
                seed_names = []
            missing = sum(1 for r in rows if r.get("embedding") is None)
            expansions[intent] = {
                "source": source,
                "rows": rows,
                "columns": ["id", "label", "text", "embedding", "freq"],
                "stats": {
                    "distinct_drugset_count": stats_count,
                    "seed_drugs": seed_names,
                    "embedding_missing": missing,
                },
            }
        else:
            expansions[intent] = {
                "source": "Unknown",
                "rows": [],
                "columns": [],
                "stats": {},
            }

    return {
        "question": search_output.get("question"),
        "seeds": seeds,
        "intents": intents,
        "topk": topk,
        "query_vector": search_output.get("query_vector"),
        "seed_values_full": {k: list(v) for k, v in seed_values_by_type.items()},
        "id_sets": {
            "Drug_ids": len(drug_ids),
            "Reaction_ids": len(reaction_ids),
            "Indication_ids": len(indication_ids),
            "Outcome_ids": len(outcome_ids),
        },
        "expansions": expansions,
        "meta": {
            "layer": "relation_aggregate",
            "version": RELATION_AGG_VERSION,
            "previous_meta": search_output.get("meta"),
            "normalization": "Drug seeds upper-cased",
            "embedding_included": True,
        },
    }
