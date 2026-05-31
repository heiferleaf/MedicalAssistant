统一 AI 外部调用链路

新增 AiHttpClient，把 RAG / Agent Flask 代理 / Predict / OCR 对 Flask 的调用统一收口。
RagService、FlaskAgentProxyService、PredictService、OcrService 不再各自散落 new RestTemplate()。
响应速度优化

FlaskClientConfig 从简单 HTTP 工厂换成 Apache HttpClient 5 连接池。
新增 ai.http.* 配置：连接超时、读超时、连接池总数、单路由连接数。
RAG 已有 Redis 缓存继续保留，命中缓存时直接返回，不再打 Flask。
压力保护 / 降级

RAG 保留本地 Semaphore bulkhead，高并发超限时返回 429 RAG_BUSY。
Agent /api/agent/chat 和 SSE 流式聊天切到 aiExecutor，队列满返回 429，处理超时返回 504，避免 AI 慢请求拖死 Web 线程。
RAG 缓存关闭时也能绕过 Redis 锁，直接走 provider + bulkhead。
评测可观测性

RAG 响应补齐 cache_hit、elapsed_ms、error_code、provider_status 这类字段。
benchmark 脚本增强了 cache_hit_rate、服务端耗时、错误码、provider 状态统计。
文档 docs/RAG治理与修复计划.md 已追加本轮已落地内容和测试口径。