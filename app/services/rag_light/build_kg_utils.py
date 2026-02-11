"""FAERS ASCII -> Neo4j 图谱构建脚本（适配云端全量导入）。

原始版本逐条 graph.create + readlines 会在全量 FAERS 数据上非常慢且易 OOM。
此版本改为：
- 流式读取（for line in f）
- 批量写入（Cypher UNWIND，每批 commit）
- 支持命令行参数：数据目录、季度文件后缀、批大小、编码

依赖：py2neo, tqdm

用法示例：
  export NEO4J_URI='bolt://127.0.0.1:7687'
  export NEO4J_USER='neo4j'
  export NEO4J_PASSWORD='xxxx'
  python3 build_kg_utils.py --data-path /srv/medicalassistant/faers_ascii/ASCII --quarter 23Q1 --batch-size 2000
"""

from __future__ import annotations

import argparse
import os
from typing import Dict, Iterable, List

from py2neo import Graph
from tqdm import tqdm


class MedicalExtractor(object):
    def __init__(self, uri: str | None = None, user: str | None = None, password: str | None = None):
        super(MedicalExtractor, self).__init__()

        uri = uri or os.getenv("NEO4J_URI", "bolt://localhost:7687")
        user = user or os.getenv("NEO4J_USER", "neo4j")
        password = password or os.getenv("NEO4J_PASSWORD")

        if not password:
            raise RuntimeError(
                "NEO4J_PASSWORD 未设置。请在环境变量中设置 NEO4J_PASSWORD，或在初始化时传入 password。"
            )

        try:
            self.graph = Graph(uri, auth=(user, password))
        except Exception as e:
            raise RuntimeError(f"连接 Neo4j 失败: {e}")

        self.invalid_primaryids = set()
        self.valid_primaryids = set()

    def ensure_schema(self):
        statements = [
            "CREATE CONSTRAINT patient_id IF NOT EXISTS FOR (n:Patient) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT drugset_id IF NOT EXISTS FOR (n:DrugSet) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT drug_id IF NOT EXISTS FOR (n:Drug) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT reaction_id IF NOT EXISTS FOR (n:Reaction) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT indication_id IF NOT EXISTS FOR (n:Indication) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT outcome_id IF NOT EXISTS FOR (n:Outcome) REQUIRE n.id IS UNIQUE",
            "CREATE INDEX patient_primaryid IF NOT EXISTS FOR (n:Patient) ON (n.primaryid)",
            "CREATE INDEX drugset_primaryid IF NOT EXISTS FOR (n:DrugSet) ON (n.primaryid)",
            "CREATE INDEX drug_primaryid IF NOT EXISTS FOR (n:Drug) ON (n.primaryid)",
            "CREATE INDEX reaction_primaryid IF NOT EXISTS FOR (n:Reaction) ON (n.primaryid)",
            "CREATE INDEX indication_primaryid IF NOT EXISTS FOR (n:Indication) ON (n.primaryid)",
            "CREATE INDEX outcome_primaryid IF NOT EXISTS FOR (n:Outcome) ON (n.primaryid)",
        ]
        for cypher in statements:
            self.graph.run(cypher)

    @staticmethod
    def _safe_join(data_path: str, filename: str) -> str:
        return os.path.join(data_path, filename)

    @staticmethod
    def _iter_lines(file_path: str, encoding: str) -> Iterable[str]:
        with open(file_path, "r", encoding=encoding, errors="ignore") as f:
            for line in f:
                yield line

    def _write_batch(self, cypher: str, rows: List[Dict]):
        if not rows:
            return
        self.graph.run(cypher, rows=rows)

    def _delete_invalid_primaryids(self, primaryids: List[str]):
        if not primaryids:
            return
        cypher = """
        UNWIND $primaryids AS pid
        MATCH (n {primaryid: pid})
        DETACH DELETE n
        """
        self.graph.run(cypher, primaryids=primaryids)

    def extract_triples(
        self,
        data_path: str,
        quarter: str,
        batch_size: int = 2000,
        encoding: str = "utf-8",
        delete_invalid: bool = True,
    ):
        self.ensure_schema()

        def filename(prefix: str) -> str:
            return f"{prefix}{quarter}.txt"

        # 1) DEMO：Patient、DrugSet、USED_IN_CASE
        demo_path = self._safe_join(data_path, filename("DEMO"))
        demo_rows: List[Dict] = []
        demo_cypher = """
        UNWIND $rows AS row
        MERGE (p:Patient {id: row.patient_id})
        SET p.primaryid = row.primaryid,
            p.caseid = row.caseid,
            p.event_dt = row.event_dt,
            p.age = row.age,
            p.age_grp = row.age_grp,
            p.sex = row.sex,
            p.wt = row.wt,
            p.occr_country = row.occr_country
        MERGE (ds:DrugSet {id: row.drugset_id})
        SET ds.primaryid = row.primaryid,
            ds.caseid = row.caseid
        MERGE (p)-[:USED_IN_CASE]->(ds)
        """
        required_indices = [0, 1, 4, 13, 16, 18, 24]
        for line in tqdm(self._iter_lines(demo_path, encoding), ncols=100, desc="DEMO -> Patient/DrugSet"):
            fields = line.rstrip("\n").split("$")
            if len(fields) <= max(required_indices):
                continue
            if any(not fields[i].strip() for i in required_indices):
                if fields[0].strip():
                    self.invalid_primaryids.add(fields[0])
                continue

            primaryid = fields[0]
            caseid = fields[1]
            patient_id = f"{primaryid}:{caseid}"
            demo_rows.append(
                {
                    "patient_id": patient_id,
                    "drugset_id": patient_id,
                    "primaryid": primaryid,
                    "caseid": caseid,
                    "event_dt": fields[4],
                    "age": fields[13],
                    "age_grp": fields[15] if len(fields) > 15 else "",
                    "sex": fields[16],
                    "wt": fields[18],
                    "occr_country": fields[24],
                }
            )
            self.valid_primaryids.add(primaryid)
            if len(demo_rows) >= batch_size:
                self._write_batch(demo_cypher, demo_rows)
                demo_rows.clear()
        self._write_batch(demo_cypher, demo_rows)

        # 2) DRUG：Drug、CONTAINS_DRUG
        drug_path = self._safe_join(data_path, filename("DRUG"))
        drug_rows: List[Dict] = []
        drug_cypher = """
        UNWIND $rows AS row
        MERGE (d:Drug {id: row.drug_id})
        SET d.primaryid = row.primaryid,
            d.caseid = row.caseid,
            d.drugname = row.drugname,
            d.prod_ai = row.prod_ai,
            d.dose_amt = row.dose_amt,
            d.dose_unit = row.dose_unit,
            d.dose_freq = row.dose_freq
        MERGE (ds:DrugSet {id: row.drugset_id})
        SET ds.primaryid = row.primaryid,
            ds.caseid = row.caseid
        MERGE (ds)-[:CONTAINS_DRUG]->(d)
        """
        # 注意：FAERS DRUG 表中 prod_ai（fields[5]）常见为空；不应因此把整个 case 作废。
        required_drug_indices = [0, 1, 4]
        for line in tqdm(self._iter_lines(drug_path, encoding), ncols=100, desc="DRUG -> Drug/CONTAINS_DRUG"):
            fields = line.rstrip("\n").split("$")
            if len(fields) <= max(required_drug_indices):
                continue
            primaryid = fields[0]
            if primaryid in self.invalid_primaryids:
                continue
            if any(not fields[i].strip() for i in required_drug_indices):
                continue

            caseid = fields[1]
            drugname = fields[4]
            drug_id = f"{primaryid}:{caseid}:{drugname}"
            drug_rows.append(
                {
                    "drug_id": drug_id,
                    "drugset_id": f"{primaryid}:{caseid}",
                    "primaryid": primaryid,
                    "caseid": caseid,
                    "drugname": drugname,
                    "prod_ai": fields[5] if len(fields) > 5 else "",
                    "dose_amt": fields[16] if len(fields) > 16 else "",
                    "dose_unit": fields[17] if len(fields) > 17 else "",
                    "dose_freq": fields[19] if len(fields) > 19 else "",
                }
            )
            if len(drug_rows) >= batch_size:
                self._write_batch(drug_cypher, drug_rows)
                drug_rows.clear()
        self._write_batch(drug_cypher, drug_rows)

        # 3) REAC：Reaction、CAUSES_REACTION
        reac_path = self._safe_join(data_path, filename("REAC"))
        reac_rows: List[Dict] = []
        reac_cypher = """
        UNWIND $rows AS row
        MERGE (r:Reaction {id: row.reaction_id})
        SET r.primaryid = row.primaryid,
            r.caseid = row.caseid,
            r.reac = row.reac
        MERGE (ds:DrugSet {id: row.drugset_id})
        SET ds.primaryid = row.primaryid,
            ds.caseid = row.caseid
        MERGE (ds)-[:CAUSES_REACTION]->(r)
        """
        for line in tqdm(self._iter_lines(reac_path, encoding), ncols=100, desc="REAC -> Reaction/CAUSES_REACTION"):
            fields = line.rstrip("\n").split("$")
            if len(fields) < 3:
                continue
            primaryid = fields[0]
            if primaryid in self.invalid_primaryids or primaryid not in self.valid_primaryids:
                continue
            caseid = fields[1]
            reac = fields[2]
            reac_rows.append(
                {
                    "reaction_id": f"{primaryid}:{caseid}:{reac}",
                    "drugset_id": f"{primaryid}:{caseid}",
                    "primaryid": primaryid,
                    "caseid": caseid,
                    "reac": reac,
                }
            )
            if len(reac_rows) >= batch_size:
                self._write_batch(reac_cypher, reac_rows)
                reac_rows.clear()
        self._write_batch(reac_cypher, reac_rows)

        # 4) OUTC：Outcome、HAS_OUTCOME
        outc_path = self._safe_join(data_path, filename("OUTC"))
        outc_rows: List[Dict] = []
        outc_cypher = """
        UNWIND $rows AS row
        MERGE (o:Outcome {id: row.outcome_id})
        SET o.primaryid = row.primaryid,
            o.caseid = row.caseid,
            o.outccode = row.outccode
        MERGE (p:Patient {id: row.patient_id})
        SET p.primaryid = row.primaryid,
            p.caseid = row.caseid
        MERGE (p)-[:HAS_OUTCOME]->(o)
        """
        for line in tqdm(self._iter_lines(outc_path, encoding), ncols=100, desc="OUTC -> Outcome/HAS_OUTCOME"):
            fields = line.rstrip("\n").split("$")
            if len(fields) < 3:
                continue
            primaryid = fields[0]
            if primaryid in self.invalid_primaryids or primaryid not in self.valid_primaryids:
                continue
            caseid = fields[1]
            outccode = fields[2]
            outc_rows.append(
                {
                    "outcome_id": f"{primaryid}:{caseid}:{outccode}",
                    "patient_id": f"{primaryid}:{caseid}",
                    "primaryid": primaryid,
                    "caseid": caseid,
                    "outccode": outccode,
                }
            )
            if len(outc_rows) >= batch_size:
                self._write_batch(outc_cypher, outc_rows)
                outc_rows.clear()
        self._write_batch(outc_cypher, outc_rows)

        # 5) INDI：Indication、TREATS_FOR
        indi_path = self._safe_join(data_path, filename("INDI"))
        indi_rows: List[Dict] = []
        indi_cypher = """
        UNWIND $rows AS row
        MERGE (i:Indication {id: row.indication_id})
        SET i.primaryid = row.primaryid,
            i.caseid = row.caseid,
            i.indi = row.indi
        MERGE (ds:DrugSet {id: row.drugset_id})
        SET ds.primaryid = row.primaryid,
            ds.caseid = row.caseid
        MERGE (ds)-[:TREATS_FOR]->(i)
        """
        for line in tqdm(self._iter_lines(indi_path, encoding), ncols=100, desc="INDI -> Indication/TREATS_FOR"):
            fields = line.rstrip("\n").split("$")
            if len(fields) < 4:
                continue
            primaryid = fields[0]
            if primaryid in self.invalid_primaryids or primaryid not in self.valid_primaryids:
                continue
            caseid = fields[1]
            indi = fields[3]
            indi_rows.append(
                {
                    "indication_id": f"{primaryid}:{caseid}:{indi}",
                    "drugset_id": f"{primaryid}:{caseid}",
                    "primaryid": primaryid,
                    "caseid": caseid,
                    "indi": indi,
                }
            )
            if len(indi_rows) >= batch_size:
                self._write_batch(indi_cypher, indi_rows)
                indi_rows.clear()
        self._write_batch(indi_cypher, indi_rows)

        print(f"完成：invalid_primaryids={len(self.invalid_primaryids)}，valid_primaryids={len(self.valid_primaryids)}")


def main():
    parser = argparse.ArgumentParser(description="Build FAERS KG into Neo4j")
    parser.add_argument("--data-path", required=True, help="ASCII 文件目录（包含 DEMOxxQx.txt 等）")
    parser.add_argument("--quarter", required=True, help="季度后缀，例如 23Q1")
    parser.add_argument("--batch-size", type=int, default=2000, help="每批写入行数")
    parser.add_argument("--encoding", default="utf-8", help="文件编码，默认 utf-8")
    parser.add_argument(
        "--no-delete-invalid",
        action="store_true",
        help="不在导入过程中删除无效 primaryid 对应的节点（更快但会残留无效数据）",
    )
    args = parser.parse_args()

    extractor = MedicalExtractor()
    extractor.extract_triples(
        data_path=args.data_path,
        quarter=args.quarter,
        batch_size=args.batch_size,
        encoding=args.encoding,
        delete_invalid=not args.no_delete_invalid,
    )


if __name__ == "__main__":
    main()
