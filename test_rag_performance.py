import time
from app.services.rag_light.rag_query import rag_query

# 测试问题
question = "如何治疗高血压？"

# 执行带时间统计的查询
print("开始测试RAG查询性能...")
t0 = time.time()
result = rag_query(
    question,
    with_trace=False,
    with_timing=True,
    suppress_internal_logs=True
)
total_time = time.time() - t0

print(f"\n总执行时间: {total_time:.2f}秒")
print("\n各步骤执行时间:")
for step, duration in result.get('timings', {}).items():
    print(f"{step}: {duration:.2f}秒")

print("\n查询结果:")
print(f"成功: {result.get('success')}")
print(f"回答: {result.get('answer')[:200]}...")
