import time
from app.services.rag_light.rag_query import rag_query
from app.services.rag_light.core.input_layer import parse_input
from app.services.rag_light.core.initial_search_layer import initial_search
from app.services.rag_light.core.relation_aggregate_layer import aggregate_relations
from app.services.rag_light.core.ranking_layer import rank_expansions
from app.services.rag_light.core.output_layer import generate_answer

# 测试问题
question = "如何治疗高血压？"

print("=== RAG查询性能测试 ===")
print(f"测试问题: {question}")
print("\n开始执行完整RAG查询...")

# 测试完整RAG查询
total_start = time.time()
result = rag_query(
    question,
    with_trace=True,
    with_timing=True,
    suppress_internal_logs=False
)
total_end = time.time()

print(f"\n总执行时间: {total_end - total_start:.2f}秒")

# 输出各环节时间
if 'timings' in result:
    print("\n各环节执行时间:")
    for step, duration in result['timings'].items():
        print(f"{step}: {duration:.2f}秒")

# 输出查询结果
print("\n查询结果:")
print(f"成功: {result.get('success')}")
print(f"回答: {result.get('answer')[:300]}...")

# 单独测试每个环节
print("\n=== 单独测试各个环节 ===")

# 1. 测试输入解析
print("\n1. 测试输入解析层...")
parse_start = time.time()
parsed = parse_input(question)
parse_end = time.time()
print(f"输入解析时间: {parse_end - parse_start:.2f}秒")
print(f"解析结果: 识别到 {len(parsed.get('seeds', []))} 个种子，{len(parsed.get('intents', []))} 个意图")

# 2. 测试初始搜索
print("\n2. 测试初始搜索层...")
search_start = time.time()
searched = initial_search(parsed)
search_end = time.time()
print(f"初始搜索时间: {search_end - search_start:.2f}秒")
print(f"搜索结果: 找到 {sum(len(v) for v in searched.get('candidates', {}).values())} 个候选节点")

# 3. 测试关系聚合
print("\n3. 测试关系聚合层...")
aggregate_start = time.time()
aggregated = aggregate_relations(searched)
aggregate_end = time.time()
print(f"关系聚合时间: {aggregate_end - aggregate_start:.2f}秒")
print(f"聚合结果: 生成 {len(aggregated.get('expansions', {}))} 个扩展")

# 4. 测试排序
print("\n4. 测试排序层...")
rank_start = time.time()
ranked = rank_expansions(aggregated)
rank_end = time.time()
print(f"排序时间: {rank_end - rank_start:.2f}秒")

# 5. 测试回答生成
print("\n5. 测试回答生成层...")
generate_start = time.time()
answer_dict = generate_answer(ranked)
generate_end = time.time()
print(f"回答生成时间: {generate_end - generate_start:.2f}秒")
print(f"生成的回答: {answer_dict.get('answer', '')[:200]}...")

print("\n=== 测试完成 ===")
