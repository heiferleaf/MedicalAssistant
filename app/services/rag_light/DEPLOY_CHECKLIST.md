# RAG 全量部署清单（Ubuntu 22.04）

> 目标：把**已迁移到 Flask 分支的全量 RAG**搬到云服务器，并通过 Flask 对外提供 /rag/query 接口，便于本地联通测试。

## 1. 基础环境
- 系统：Ubuntu 22.04 LTS
- 运行时：Python 3.10+（建议 3.10/3.11）
- Java 21（用于 Spring Boot）
- Git
- 基础工具：curl、wget、unzip

## 2. 代码与目录
- 拉取 Flask 仓库（已包含迁移后的全量 RAG：`app/services/rag_light`）
- 不再拉取 FAERS_AGENT/rag（科研原版不直接上云）
- 建议目录布局（示例）：
  - /srv/medicalassistant/flask

## 3. Neo4j（知识图谱）
- 安装 Neo4j（与现有数据版本匹配）
- 配置访问账号与端口（Bolt 7687）
- 导入完整图数据（Drug/Reaction/Indication/Outcome 等）

### 3.1 数据导入（txt + 脚本）
1) 把本地 FAERS ASCII txt 上传到服务器
  - 建议目录：`/srv/medicalassistant/faers_ascii/ASCII/`
  - 目录内文件名需匹配：`DEMO{季度}.txt`、`DRUG{季度}.txt`、`REAC{季度}.txt`、`OUTC{季度}.txt`、`INDI{季度}.txt`
  - 示例：`DEMO23Q1.txt`、`DRUG23Q1.txt`...

2) 使用仓库内脚本导入 Neo4j
  - 脚本位置：`app/services/rag_light/build_kg_utils.py`
  - 依赖：`py2neo`、`tqdm`
    - 推荐只安装导入所需最小依赖：`pip install -r app/services/rag_light/requirements.kg.txt`
    - （可选）若你在安装全量 `requirements.txt` 时遇到 pip 的 `AssertionError`，先升级 pip 再重试：
      - `python -m pip install -U pip setuptools wheel`
      - 并建议使用 https 的阿里云镜像：`pip install -r requirements.txt -i https://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com`
  - 运行前设置环境变量（推荐走本机回环，避免公网链路）：
    - `export NEO4J_URI='bolt://127.0.0.1:7687'`
    - `export NEO4J_USER='neo4j'`
    - `export NEO4J_PASSWORD='你的Neo4j密码'`
  - 执行示例：
    - `python3 app/services/rag_light/build_kg_utils.py --data-path /srv/medicalassistant/faers_ascii/ASCII --quarter 23Q1 --batch-size 2000`

3) 导入完成后，用 Neo4j Browser 验证
  - 节点统计：`MATCH (n) RETURN labels(n), count(*) ORDER BY count(*) DESC;`
  - 关系统计：`MATCH ()-[r]->() RETURN type(r), count(*) ORDER BY count(*) DESC;`

## 4. RAG 运行依赖（已迁移到 Flask）
- Python 依赖：requests、sentence-transformers、py2neo 等
- 环境变量：
  - OPENAI/Chat API Key（输入解析层）
  - EMBEDDING_MODEL_TYPE / BIENCODER_MODEL_PATH（向量模型）
  - Ollama 或外部 LLM API（答案生成层）

## 5. 模型与数据
- 双塔模型权重（如 `models/drug2reaction_biencoder_trial`）
- 评估/统计数据（如 `data/*`）
- embedding 需与 Neo4j 中节点向量一致

## 6. LLM 答案生成策略
- 方案A：本地 Ollama（CPU 版本，小模型）
- 方案B：云端 LLM API（推荐，稳定）

### 6.1 Ollama 安装与“下载加速”（SSH 反向隧道 + 本机 Clash 代理）
适用场景：云服务器直连下载 Ollama / 模型很慢，但你本机网络正常且已开启 Clash 代理（例如 `127.0.0.1:7897`）。

1) 在你本机（Windows）开启一条 SSH 反向隧道（此命令会“卡住不动”是正常的）
  - PowerShell：
    - `ssh -N -R 7897:127.0.0.1:7897 root@<你的服务器IP>`

2) 在云服务器上验证代理是否可用
  - `curl -I -x http://127.0.0.1:7897 https://ollama.com`
  - 看到 `HTTP/1.1 200 Connection established` 且后续有 `HTTP/2 200`，说明链路已通。

3) 在云服务器上通过该代理安装/拉取模型（只对当前 shell 生效）
  - `export http_proxy=http://127.0.0.1:7897`
  - `export https_proxy=http://127.0.0.1:7897`
  - `curl -fsSL https://ollama.com/install.sh | sh`
  - `ollama --version`
  - 拉取模型示例：
    - `ollama pull nomic-embed-text`
    - `ollama pull qwen2.5:3b-instruct`（资源紧张先用 3b）
    - （可选）`ollama pull qwen2.5:7b-instruct`

4) 代理不用时可取消（避免影响其它下载）
  - `unset http_proxy https_proxy`

5) 注意事项
  - 如果你用的是非 root 用户登录，把上面的 `root@...` 换成实际账号即可。
  - 如果 Clash 端口不是 `7897`，以你本机 Clash Verge 显示的 System Proxy 端口为准。
  - 这套方法只解决“下载慢”；Ollama 推理速度仍取决于云服务器 CPU/GPU。

## 7. Flask 接入
- Flask /rag/query 使用迁移后的 rag_light
- 设置：Neo4j 与模型环境变量（见 .env.example）
- 暴露端口（默认 8001）

### 7.1 Path B（本机 Ollama：embedding + chat）推荐环境变量
- Embedding：
  - `EMBEDDING_MODEL_TYPE=ollama`
  - `OLLAMA_BASE_URL=http://127.0.0.1:11434`
  - `OLLAMA_EMBED_MODEL=nomic-embed-text`
- 输入解析：
  - `INPUT_PROVIDER=ollama`（不依赖 OPENAI_API_KEY）
- 答案生成：
  - `LLM_PROVIDER=ollama`
  - `OLLAMA_CHAT_MODEL=qwen2.5:3b-instruct`（或 7b）

兼容说明：历史上 embedding/chat 共用 `OLLAMA_MODEL`。现在建议使用 `OLLAMA_EMBED_MODEL` 与 `OLLAMA_CHAT_MODEL` 分开配置。

## 8. Spring Boot 接入
- 配置 ai.rag.base-url 指向 Flask
- 本地 Postman 验证通路

## 9. 端到端联通测试
- 直接访问 Flask /rag/query
- 通过 Spring Boot /api/ai/rag/query

---

## 需要搬上去的内容清单（按优先级）
1) Flask 服务代码（当前分支，含 rag_light 全量逻辑）
2) Neo4j 安装包与数据导入脚本
3) 本地 txt 数据文件（导入 Neo4j）
4) 双塔模型权重（models/）
5) 运行所需环境变量与配置文件
6) （可选）Ollama 模型或云端 LLM API Key

---

如果你确认这个清单，我会继续补充“逐步部署步骤”。
