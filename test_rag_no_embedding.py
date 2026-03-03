import time
import sys
import os

# 设置环境变量，禁用向量嵌入
os.environ['USE_EMBEDDING'] = 'false'

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# 直接导入RAG相关模块
from app.services.rag_light.rag_query import rag_query

# 测试问题
question = "如何治疗高血压？"

print("=== RAG查询性能测试（禁用向量）===")
print(f"测试问题: {question}")
print("\n开始执行RAG查询...")

# 测试RAG查询
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
print(f"回答: {result.get('answer', '')[:300]}...")

# 输出元数据
print("\n元数据:")
if 'meta' in result:
    print(f"输入元数据: {result['meta'].get('input_meta', {})}")

print("\n=== 测试完成 ===")
