# ============================================================
# Prodigal Docker Image
# 
# 用于 MAG (宏基因组组装基因组) 基因预测
# 将原始核酸序列转换为蛋白质序列
# ============================================================

FROM ubuntu:22.04

# 设置环境变量
ENV DEBIAN_FRONTEND=noninteractive

# 安装 Prodigal
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        prodigal \
        ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /data

# 入口点
ENTRYPOINT ["prodigal"]

# 默认显示帮助
CMD ["-h"]
