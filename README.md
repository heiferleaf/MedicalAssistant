# Flask OCR 服务 - Docker 部署

用于承载 AI 相关服务的 Flask 轻量接口层（独立分支：flask-service）。

## 快速部署

### 1. 配置环境变量

```bash
# 创建 .env 文件
export DASHSCOPE_API_KEY="your_api_key"
export MYSQL_PASSWORD="App123456"
```

### 2. 构建并启动

```bash
# 方式一：使用 docker-compose（推荐）
docker-compose up -d --build

# 方式二：直接构建和运行
docker build -t medicalassistant-flask:latest .
docker run -d -p 8001:8001 --name medicalassistant-flask \
  -e DASHSCOPE_API_KEY="$DASHSCOPE_API_KEY" \
  -e MYSQL_PASSWORD="$MYSQL_PASSWORD" \
  medicalassistant-flask:latest
```

### 3. 验证部署

```bash
# 查看状态
docker ps

# 查看日志
docker logs -f medicalassistant-flask

# 测试健康检查
curl http://localhost:8001/health
```

## 常用命令

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f

# 进入容器
docker exec -it medicalassistant-flask bash

# 重新构建
docker-compose build --no-cache
```

## 配置说明

### 必需环境变量
- `DASHSCOPE_API_KEY`: 通义千问 API 密钥
- `MYSQL_PASSWORD`: MySQL 数据库密码

### 可选环境变量
- `MYSQL_HOST`: MySQL 主机地址（默认：host.docker.internal）
- `MYSQL_PORT`: MySQL 端口（默认：3306）
- `MYSQL_DB`: 数据库名（默认：medicalassistant）
- `MYSQL_USER`: 数据库用户名（默认：appuser）
- `NEO4J_URI`: Neo4j 数据库地址
- `OLLAMA_BASE_URL`: Ollama 服务地址

## 注意事项

1. **模型文件**：通过 volume 挂载到容器，避免镜像过大
2. **数据库连接**：使用 `host.docker.internal` 访问宿主机 MySQL
3. **端口映射**：默认使用 8001 端口，可通过 `-p` 参数修改

## 故障排查

```bash
# 查看容器状态
docker ps -a

# 查看构建缓存
docker builder prune

# 清理未使用的资源
docker system prune -a
```
