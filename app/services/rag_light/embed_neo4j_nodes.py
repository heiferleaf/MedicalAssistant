"""Backfill embeddings for Neo4j nodes.

This script computes embeddings for KG nodes (Drug/Reaction/Indication/Outcome)
and stores them on each node as `embedding: List[float]`.

Default embedding provider is controlled by env vars used in
`app.services.rag_light.core.embedding_utils` (e.g. OLLAMA_BASE_URL,
OLLAMA_EMBED_MODEL, EMBEDDING_MODEL_TYPE).

Example:
  export NEO4J_URI='bolt://127.0.0.1:7687'
  export NEO4J_USER='neo4j'
  export NEO4J_PASSWORD='xxxx'
  export OLLAMA_BASE_URL='http://127.0.0.1:11434'
  export OLLAMA_EMBED_MODEL='nomic-embed-text'
  python3 -m app.services.rag_light.embed_neo4j_nodes --labels Drug Reaction --batch-size 128
"""

from __future__ import annotations

import argparse
import os
import sys
import time
from typing import Dict, Iterable, List, Tuple

from py2neo import Graph
from tqdm import tqdm


LABEL_PROP: Dict[str, str] = {
    "Drug": "drugname",
    "Reaction": "reac",
    "Indication": "indi",
    "Outcome": "outccode",
}


def _load_env_file(path: str, *, override: bool = False) -> None:
    """Load KEY=VALUE lines from a .env-like file into os.environ (if absent).

    This is intentionally minimal (no variable expansion).
    """

    if not path:
        return
    if not os.path.exists(path):
        return

    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            for raw in f:
                line = raw.strip()
                if not line or line.startswith("#"):
                    continue
                if "=" not in line:
                    continue
                key, value = line.split("=", 1)
                key = key.strip()
                if key.startswith("export "):
                    key = key[len("export ") :].strip()
                value = value.strip().strip("\"").strip("'")
                if not key:
                    continue
                if override or key not in os.environ:
                    os.environ[key] = value
    except Exception:
        # Best-effort: if env loading fails, we'll surface missing vars later.
        return


def _connect_graph() -> Graph:
    uri = os.getenv("NEO4J_URI", "bolt://127.0.0.1:7687")
    user = os.getenv("NEO4J_USER", "neo4j")
    password = os.getenv("NEO4J_PASSWORD", "")
    if not password:
        raise RuntimeError(
            "NEO4J_PASSWORD 未设置。请：\n"
            "1) export NEO4J_PASSWORD=... 后再运行；或\n"
            "2) 把 NEO4J_PASSWORD 写入 /etc/medicalassistant/flask.env；或\n"
            "3) 设置 NEO4J_ENV_FILE 指向你的 env 文件。"
        )
    return Graph(uri, auth=(user, password))


def _iter_missing(graph: Graph, label: str, prop: str, limit: int | None) -> Iterable[Tuple[int, str]]:
    q = f"""
    MATCH (n:{label})
    WHERE n.{prop} IS NOT NULL AND trim(toString(n.{prop})) <> ''
    RETURN id(n) AS nid, toString(n.{prop}) AS text
    ORDER BY nid ASC
    {"LIMIT $limit" if limit is not None else ""}
    """
    rows = graph.run(q, limit=limit).data() if limit is not None else graph.run(q).data()
    for r in rows:
        yield int(r["nid"]), str(r["text"])


def _write_batch(graph: Graph, label: str, rows: List[Dict]) -> None:
    if not rows:
        return
    q = f"""
    UNWIND $rows AS row
    MATCH (n:{label}) WHERE id(n) = row.nid
    SET n.embedding = row.embedding
    """
    graph.run(q, rows=rows)


def backfill_label(
    graph: Graph,
    label: str,
    *,
    embed_fn,
    batch_size: int,
    limit: int | None,
    sleep_ms: int,
) -> Dict[str, int]:
    if label not in LABEL_PROP:
        raise ValueError(f"不支持的 label: {label}")

    prop = LABEL_PROP[label]
    pending: List[Dict] = []
    total = 0
    ok = 0
    failed = 0
    first_error: str | None = None

    for nid, text in tqdm(list(_iter_missing(graph, label, prop, limit)), ncols=100, desc=f"embed {label}"):
        total += 1
        try:
            emb = embed_fn(text)
            pending.append({"nid": nid, "embedding": emb})
            ok += 1
        except Exception as exc:  # noqa: BLE001
            failed += 1
            if first_error is None:
                first_error = repr(exc)

        if len(pending) >= batch_size:
            _write_batch(graph, label, pending)
            pending.clear()
            if sleep_ms > 0:
                time.sleep(sleep_ms / 1000.0)

    _write_batch(graph, label, pending)

    stats: Dict[str, int] = {"total": total, "ok": ok, "failed": failed}
    if first_error:
        # Provide one representative failure reason for quick diagnosis.
        print(f"[embed] {label} first_error={first_error}")
    return stats


def main(argv: List[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--labels",
        nargs="+",
        default=["Drug", "Reaction", "Indication", "Outcome"],
        help="要回填 embedding 的标签列表",
    )
    parser.add_argument("--batch-size", type=int, default=128)
    parser.add_argument("--limit", type=int, default=None, help="每个 label 最多处理多少条（调试用）")
    parser.add_argument("--sleep-ms", type=int, default=0, help="每批写入后休眠毫秒数")
    args = parser.parse_args(argv)

    # Load env file early so embedding/ollama settings are visible during imports.
    env_file = os.getenv("NEO4J_ENV_FILE", "/etc/medicalassistant/flask.env")
    _load_env_file(env_file, override=True)

    # Helpful diagnostics: show which embedding config is actually active.
    print(f"[embed] env_file={env_file}")
    print(f"[embed] EMBEDDING_MODEL_TYPE={os.getenv('EMBEDDING_MODEL_TYPE')}")
    print(f"[embed] OLLAMA_BASE_URL={os.getenv('OLLAMA_BASE_URL')}")
    print(f"[embed] OLLAMA_EMBED_MODEL={os.getenv('OLLAMA_EMBED_MODEL') or os.getenv('OLLAMA_MODEL')}")

    from app.services.rag_light.core.embedding_utils import embed_text, get_current_model_name

    graph = _connect_graph()
    model = get_current_model_name()
    print(f"[embed] embedding_model={model}")

    summary: Dict[str, Dict[str, int]] = {}
    for label in args.labels:
        summary[label] = backfill_label(
            graph,
            label,
            embed_fn=embed_text,
            batch_size=args.batch_size,
            limit=args.limit,
            sleep_ms=args.sleep_ms,
        )

    print("[embed] summary:")
    for label, stats in summary.items():
        print(f"  - {label}: total={stats['total']} ok={stats['ok']} failed={stats['failed']}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
