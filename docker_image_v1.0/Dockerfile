# ============================================================
# ARG-BiLSTM Docker Image
# 
# 用于抗性基因(ARG)识别与分类
# ============================================================

# 基础镜像 (CPU版本，体积较小)
FROM python:3.10-slim

# 设置工作目录
WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY ./app /app
COPY ./models /app/models

# 环境变量
ENV PYTHONPATH=/app
ENV PYTHONUNBUFFERED=1

# 入口点
ENTRYPOINT ["python", "/app/cli.py"]

# 默认显示帮助
CMD ["--help"]

