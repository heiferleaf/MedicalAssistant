// Neo4j index initialization for drug-reaction graph (FAERS data)
// Run once after neo4j-medical container starts.
// All statements are idempotent (IF NOT EXISTS).
//
// Usage:
//   cat scripts/neo4j_init_indexes.cypher | \
//     cypher-shell -u neo4j -p password --format plain
// Or via HTTP:
//   curl -s -X POST http://localhost:7474/db/neo4j/tx/commit \
//     -H "Content-Type: application/json" -u "neo4j:password" \
//     -d '{"statements":[{"statement":"<each line below>"}]}'

// Drug.drugname — used in every drug lookup query
CREATE INDEX drug_name_idx IF NOT EXISTS FOR (d:Drug) ON (d.drugname);

// Reaction.reac — used in ORDER BY / WHERE filters on reaction name
CREATE INDEX reaction_reac_idx IF NOT EXISTS FOR (r:Reaction) ON (r.reac);

// Indication.indi_pt — used in indication lookup queries
CREATE INDEX indication_indi_pt_idx IF NOT EXISTS FOR (i:Indication) ON (i.indi_pt);
