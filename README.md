# MedicalAssistant Flask Service

用于承载 AI 相关服务的 Flask 轻量接口层（独立分支：flask-service）。

## 目录结构
```
app/
	routes/         # 路由层（rag/agent/ocr 等）
	services/       # 业务服务层
	utils/          # 通用工具
app.py            # 应用入口
```

## 功能
- `GET /health` 健康检查
- `POST /rag/query` RAG 代理接口（通过 `RAG_PROXY_URL` 透传）
- `GET /agent/health` Agent 预留健康检查
- `GET /ocr/health` OCR 预留健康检查

## 轻量 RAG 模式
默认开启轻量 RAG（无 Neo4j / 无本地大模型）：
- 使用本地 JSON 语料做检索
- 调用云端 LLM API 生成回答

如需关闭轻量模式并切回科研版 RAG：
- 设置 `RAG_LIGHT_MODE=false`
- 并提供 `RAG_PROJECT_PATH` 或 `RAG_PROXY_URL`

## 本地启动
```
pip install -r requirements.txt
python app.py
```

## 环境变量
复制并修改 `.env.example` 为 `.env`（或直接设置系统环境变量）：
- `RAG_PROXY_URL`：实际 RAG 服务地址
- `FLASK_PORT`：服务端口（默认 8001）
