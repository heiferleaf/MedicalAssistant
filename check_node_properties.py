from py2neo import Graph

g = Graph("bolt://localhost:7687", auth=("neo4j", "12345678"))

# Check Drug node properties
drug_nodes = list(g.nodes.match("Drug").limit(3))
print("Drug node properties:")
for d in drug_nodes:
    print(f"  ID: {d.identity}")
    print(f"  All properties: {dict(d)}")
    print()

# Check Reaction node properties
reaction_nodes = list(g.nodes.match("Reaction").limit(3))
print("Reaction node properties:")
for r in reaction_nodes:
    print(f"  ID: {r.identity}")
    print(f"  All properties: {dict(r)}")
    print()
