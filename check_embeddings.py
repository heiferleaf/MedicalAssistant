from py2neo import Graph

g = Graph("bolt://localhost:7687", auth=("neo4j", "12345678"))

# Check Drug nodes
drug_nodes = list(g.nodes.match("Drug").limit(5))
print("Drug nodes sample:")
for d in drug_nodes:
    embedding_value = d.get("embedding")
    has_embedding = embedding_value is not None and isinstance(embedding_value, list) and len(embedding_value) > 0
    print(f"  ID: {d.identity}, Name: {d.get('drugname', 'N/A')}, Has embedding: {has_embedding}")
    if has_embedding:
        print(f"    Embedding dimension: {len(embedding_value)}")

# Check Reaction nodes
reaction_nodes = list(g.nodes.match("Reaction").limit(5))
print("\nReaction nodes sample:")
for r in reaction_nodes:
    embedding_value = r.get("embedding")
    has_embedding = embedding_value is not None and isinstance(embedding_value, list) and len(embedding_value) > 0
    print(f"  ID: {r.identity}, Name: {r.get('reac', 'N/A')}, Has embedding: {has_embedding}")
    if has_embedding:
        print(f"    Embedding dimension: {len(embedding_value)}")

# Check Indication nodes
indication_nodes = list(g.nodes.match("Indication").limit(5))
print("\nIndication nodes sample:")
for i in indication_nodes:
    embedding_value = i.get("embedding")
    has_embedding = embedding_value is not None and isinstance(embedding_value, list) and len(embedding_value) > 0
    print(f"  ID: {i.identity}, Name: {i.get('indi', 'N/A')}, Has embedding: {has_embedding}")
    if has_embedding:
        print(f"    Embedding dimension: {len(embedding_value)}")

# Check Outcome nodes
outcome_nodes = list(g.nodes.match("Outcome").limit(5))
print("\nOutcome nodes sample:")
for o in outcome_nodes:
    embedding_value = o.get("embedding")
    has_embedding = embedding_value is not None and isinstance(embedding_value, list) and len(embedding_value) > 0
    print(f"  ID: {o.identity}, Name: {o.get('outccode', 'N/A')}, Has embedding: {has_embedding}")
    if has_embedding:
        print(f"    Embedding dimension: {len(embedding_value)}")
