# 使用 Python 官方镜像
FROM python:3.10-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量，防止 Python 生成 .pyc 文件和缓冲输出
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# 配置 pip 使用国内镜像源（阿里云）
RUN pip install --upgrade pip -i https://mirrors.aliyun.com/pypi/simple/

# 先复制依赖文件，利用缓存机制
COPY requirements.txt .

# 使用国内镜像源安装依赖（加速构建）
RUN pip install --no-cache-dir -r requirements.txt \
    -i https://mirrors.aliyun.com/pypi/simple/ \
    --trusted-host mirrors.aliyun.com

# 复制应用代码（排除大型模型文件）
COPY app/ ./app/
COPY app.py .

# 暴露端口
EXPOSE 8001

# 设置默认环境变量
ENV FLASK_HOST=0.0.0.0
ENV FLASK_PORT=8001
ENV FLASK_DEBUG=false

# 启动应用
CMD ["python", "app.py"]
