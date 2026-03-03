import time
import sys
import os
import traceback

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

# 设置环境变量，禁用向量嵌入以提高性能
os.environ['USE_EMBEDDING'] = 'false'
# 设置使用本地Ollama模型
os.environ['AI_PROFILE'] = 'local'

# 直接导入RAG相关模块
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
print(f"使用本地Ollama模型: {os.getenv('AI_PROFILE') == 'local'}")
print(f"禁用向量嵌入: {os.getenv('USE_EMBEDDING') == 'false'}")
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

# 单独测试各个环节
print("\n=== 单独测试各个环节 ===")

# 1. 测试输入解析
print("\n1. 测试输入解析层...")
parse_start = time.time()
try:
    parsed = parse_input(question)
    parse_end = time.time()
    print(f"输入解析时间: {parse_end - parse_start:.2f}秒")
    print(f"解析结果: 识别到 {len(parsed.get('seeds', []))} 个种子，{len(parsed.get('intents', []))} 个意图")
except Exception as e:
    parse_end = time.time()
    print(f"输入解析时间: {parse_end - parse_start:.2f}秒")
    print(f"输入解析错误: {e}")
    traceback.print_exc()

# 2. 测试初始搜索
print("\n2. 测试初始搜索层...")
search_start = time.time()
try:
    searched = initial_search(parsed)
    search_end = time.time()
    print(f"初始搜索时间: {search_end - search_start:.2f}秒")
    print(f"搜索结果: 找到 {sum(len(v) for v in searched.get('candidates', {}).values())} 个候选节点")
except Exception as e:
    search_end = time.time()
    print(f"初始搜索时间: {search_end - search_start:.2f}秒")
    print(f"初始搜索错误: {e}")
    traceback.print_exc()

# 3. 测试关系聚合
print("\n3. 测试关系聚合层...")
aggregate_start = time.time()
try:
    aggregated = aggregate_relations(searched)
    aggregate_end = time.time()
    print(f"关系聚合时间: {aggregate_end - aggregate_start:.2f}秒")
    print(f"聚合结果: 生成 {len(aggregated.get('expansions', {}))} 个扩展")
except Exception as e:
    aggregate_end = time.time()
    print(f"关系聚合时间: {aggregate_end - aggregate_start:.2f}秒")
    print(f"关系聚合错误: {e}")
    traceback.print_exc()

# 4. 测试排序
print("\n4. 测试排序层...")
rank_start = time.time()
try:
    ranked = rank_expansions(aggregated)
    rank_end = time.time()
    print(f"排序时间: {rank_end - rank_start:.2f}秒")
except Exception as e:
    rank_end = time.time()
    print(f"排序时间: {rank_end - rank_start:.2f}秒")
    print(f"排序错误: {e}")
    traceback.print_exc()

# 5. 测试回答生成
print("\n5. 测试回答生成层...")
generate_start = time.time()
try:
    answer_dict = generate_answer(ranked)
    generate_end = time.time()
    print(f"回答生成时间: {generate_end - generate_start:.2f}秒")
    print(f"生成的回答: {answer_dict.get('answer', '')[:200]}...")
except Exception as e:
    generate_end = time.time()
    print(f"回答生成时间: {generate_end - generate_start:.2f}秒")
    print(f"回答生成错误: {e}")
    traceback.print_exc()

print("\n=== 测试完成 ===")
