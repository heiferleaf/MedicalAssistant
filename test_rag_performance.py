import time
import sys
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# 加载环境变量
def load_env_file():
    env_file = '/etc/medicalassistant/flask.env'
    if os.path.exists(env_file):
        with open(env_file, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    key = key.strip()
                    if key.startswith('export '):
                        key = key[7:].strip()
                    value = value.strip().strip('"').strip("'")
                    if key and key not in os.environ:
                        os.environ[key] = value

# 加载环境变量
load_env_file()

# 直接导入RAG相关模块
from app.services.rag_light.rag_query import rag_query

# 测试问题
question = "如何治疗高血压？"

print("=== RAG查询性能测试 ===")
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

print("\n=== 测试完成 ===")
