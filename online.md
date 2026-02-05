# ARG 可视化系统 — 上线部署说明

Spring Boot 后端 + Vue 3 前端，分别打包为 **jar** 与 **dist** 后的服务器运行方式。

---

## 一、本地打包命令

### 1. 前端打包（生成 dist）

```bash
cd arg_visualization_web/frontend
npm install
npm run build
```

打包完成后，静态资源在 **`arg_visualization_web/frontend/dist`** 目录。

### 2. 后端打包（生成 jar）

```bash
cd arg_visualization_web/backend
mvn clean package -DskipTests
```

打包完成后，可执行 jar 在 **`arg_visualization_web/backend/target/web-0.0.1-SNAPSHOT.jar`**。

---

## 二、服务器环境要求

- **Java 17**（运行后端 jar）
- **MySQL**（后端数据源，需提前建库）
- **Redis**（后端会话/缓存）
- （可选）**Node.js 18+**：仅当在服务器上现场执行 `npm run build` 时需要；若在本地已打好 dist，则不需要
- （可选）**Nginx**：用于托管前端静态文件并反向代理 `/api`，推荐生产环境使用

---

## 三、上传到服务器

将以下内容上传到服务器（如 `/opt/arg-web`）：

- `web-0.0.1-SNAPSHOT.jar`（后端）
- `dist/` 整个目录（前端打包结果）

```bash
# 示例：在服务器上目录结构
/opt/arg-web/
├── web-0.0.1-SNAPSHOT.jar
└── dist/
    ├── index.html
    └── assets/
```

---

## 四、服务器上运行

### 方式 A：仅用命令行（开发/测试）

**1. 启动后端（默认端口 8080）**

```bash
cd /opt/arg-web
java -jar web-0.0.1-SNAPSHOT.jar
```

如需指定配置（如数据库、Redis、端口等），可用环境变量或 `--spring.config.location`：

```bash
# 示例：指定端口、MySQL、Redis
export SERVER_PORT=8080
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/your_db?useUnicode=true&characterEncoding=utf8
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=your_password
export REDIS_HOST=127.0.0.1
java -jar web-0.0.1-SNAPSHOT.jar
```

**2. 启动前端静态服务（端口 3000，可选）**

若未用 Nginx，可用任意静态服务器托管 dist，例如：

```bash
cd /opt/arg-web/dist
npx --yes serve -l 3000 -s
```

浏览器访问：`http://服务器IP:3000`。  
此时前端请求会发往当前页面的「同域」；若页面在 `http://IP:3000`，需保证 API 也在同域或配置前端生产环境 API 地址（见下文「前端 API 地址」）。

**3. 后台运行（nohup）**

```bash
# 后端
nohup java -jar web-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &

# 前端（若用 serve）
nohup npx --yes serve -l 3000 -s dist > frontend.log 2>&1 &
```

---

### 方式 B：Nginx 托管前端 + 反向代理（推荐生产）

Nginx 提供 80/443，静态文件用 dist，`/api` 转发到后端 8080，前后端同域，无需改前端代码。

**1. 仅启动后端**

```bash
cd /opt/arg-web
nohup java -jar web-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
```

**2. Nginx 配置示例**

```nginx
server
{
    listen 80;
    server_name IP;
    index index.html;
    root /home/www/arg/dist;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
        client_max_body_size 1024M;
    }
}
```

重载 Nginx：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

浏览器访问：`http://your-domain.com` 或 `http://服务器IP`。

---

## 五、前端 API 地址（前后端不同域时）

若前端页面与后端不在同一域（例如前端 `http://IP:3000`，后端 `http://IP:8080`），需在**构建前端时**指定 API 地址：

```bash
cd arg_visualization_web/frontend
VITE_API_BASE_URL=http://你的后端地址:8080/api npm run build
```

例如：

```bash
VITE_API_BASE_URL=http://192.168.1.100:8080/api npm run build
```

使用 Nginx 同域部署时，用相对路径 `/api` 即可，无需设置 `VITE_API_BASE_URL`。

---

## 六、常用命令汇总

| 步骤           | 命令 |
|----------------|------|
| 本地打包前端   | `cd arg_visualization_web/frontend && npm install && npm run build` |
| 本地打包后端   | `cd arg_visualization_web/backend && mvn clean package -DskipTests` |
| 服务器启动后端 | `java -jar web-0.0.1-SNAPSHOT.jar` |
| 后端后台运行   | `nohup java -jar web-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &` |
| 仅测前端静态   | `cd dist && npx --yes serve -l 3000 -s` |

---

## 七、注意事项

1. **MySQL / Redis**：确保服务器已安装并启动，且在 `application.yml` 或环境变量中配置正确（如 `SPRING_DATASOURCE_*`、`REDIS_HOST` 等）。
2. **上传/输出目录**：后端中 `file.upload.genome-dir`、`analysis.output-dir` 等需在服务器上存在且可写，可通过环境变量覆盖（如 `GENOME_UPLOAD_HOST_PATH`、`ANALYSIS_OUTPUT_HOST_PATH`）。
3. **Docker**：若使用 ARG 推理、Prodigal、BLAST 等 Docker 镜像，服务器需安装 Docker 并保证镜像与配置一致。
4. **防火墙**：若直接通过端口访问，需放行 8080（后端）、3000（若用 serve）、80/443（若用 Nginx）。

按上述步骤即可在服务器上仅用命令行（及可选 Nginx）跑起前后端。

---

## 八、服务器配置评估与最佳实践

### 8.1 影响配置的主要因素

| 因素 | 说明 | 对资源的影响 |
|------|------|--------------|
| **并发用户/任务** | 同时在线人数、同时运行的分析任务数（queue-size: 100） | CPU、内存、Tomcat 线程（max 200） |
| **单次分析规模** | 单次上传的 MAG/序列数量、单文件最大 500MB | 磁盘 I/O、临时空间、Docker 运行时长 |
| **Docker 计算** | ARG 推理（可选 GPU）、Prodigal（8 线程）、BLAST（4 线程） | CPU/内存/GPU；单任务最长约 3600s |
| **数据库** | MySQL 用户/任务/日志表增长 | 内存、磁盘、连接数 |
| **Redis** | 会话、验证码、缓存 | 内存通常较小 |
| **文件存储** | 上传目录、分析结果目录 | 磁盘容量与 IOPS |

### 8.2 按组件粗估资源

- **Spring Boot（Java 17）**  
  - 建议 JVM 堆：**1～2GB** 起步（`-Xms1g -Xmx2g`），高并发再调大。  
  - Tomcat 已配置 max-threads=200、max-connections=10000，注意与 CPU 核数匹配，避免过多线程导致上下文切换。

- **MySQL**  
  - 小规模（千级用户、万级任务）：1 核 2GB 内存、20GB+ SSD 即可。  
  - 随数据量增大，优先加内存和磁盘，必要时调大 `max_connections`。

- **Redis**  
  - 本应用用量不大，256MB～512MB 内存即可。

- **Docker（ARG / Prodigal / BLAST）**  
  - **CPU 版**：Prodigal 8 线程 + BLAST 4 线程 + ARG 推理，建议 **4 核及以上**，内存 **4GB+**（容器内 PyTorch 等会占内存）。  
  - **GPU 版**：若 `ARG_USE_GPU=true`，需 NVIDIA 驱动 + 至少 1 块 GPU（显存建议 ≥4GB）。

- **磁盘**  
  - 系统 + 应用：**20GB+**。  
  - 上传 + 分析结果：按「单用户/单任务平均占用 × 预期任务数」预留，建议 **50GB～200GB+**（SSD 更佳，大文件上传/解压时 IO 密集）。

- **前端 / Nginx**  
  - 仅提供静态资源和反向代理，资源占用很小，与后端同机即可。

### 8.3 推荐配置档位（参考）

| 场景 | CPU | 内存 | 磁盘 | 说明 |
|------|-----|------|------|------|
| **演示 / 个人** | 2 核 | 4GB | 40GB SSD | 低并发、可关闭或限流部分 Docker 任务 |
| **小团队 / 正式上线** | 4 核 | 8GB | 80GB SSD | 支持少量并发分析，MySQL/Redis/应用同机 |
| **生产（推荐）** | 4～8 核 | 16GB | 150GB+ SSD | 10+ 并发任务、队列 100，Docker 全开 |
| **高并发 / 多用户** | 8 核+ | 32GB+ | 200GB+ SSD | 可考虑 MySQL/Redis 独立部署、任务队列限流 |

若使用 **GPU 推理**，在「生产」档位基础上增加 1 块 GPU（显存 ≥4GB），或选用带 GPU 的云实例。

### 8.4 评估步骤（最佳实践）

1. **先小后大**  
   - 先用 **2 核 4GB**（或云厂商最小推荐）部署整套：后端 + MySQL + Redis + Nginx + 前端。  
   - 关闭或降低 Docker 并发（如 queue-size 调小、先关 BLAST），验证功能与稳定性。

2. **压测与观察**  
   - 模拟多用户登录、上传、发起分析、查询结果。  
   - 观察：CPU、内存、磁盘 I/O、JVM 堆、Tomcat 活跃线程、MySQL 连接数、Redis 内存。  
   - 用 `top`、`htop`、`docker stats`、JVM 监控（如 `jmap`、GC 日志）和 MySQL 慢查询日志即可做初步评估。

3. **按瓶颈扩容**  
   - **CPU 常满**：升配 CPU 或增加节点（若后续做分布式）；适当限制同时运行的 Docker 任务数。  
   - **内存不足**：增大机器内存，并调大 JVM `-Xmx`（建议不超过物理内存的 70%）。  
   - **磁盘慢 / 满**：换 SSD、扩容，或把上传/结果目录放到更大盘、对象存储。  
   - **数据库慢**：优化索引、SQL，或 MySQL 独立并升配。

4. **生产前必做**  
   - 设置 **JVM 参数**：如 `-Xms1g -Xmx2g -XX:+UseG1GC`，避免默认堆过小或过大。  
   - **限流与队列**：保持 `analysis.queue-size` 与当前 CPU/内存匹配，避免同时跑过多重型任务。  
   - **日志与监控**：应用日志、Nginx 访问/错误日志、系统监控（CPU/内存/磁盘），便于事后排查与再评估。  
   - **备份**：定期备份 MySQL 与重要上传/结果目录（或同步到对象存储）。

### 8.5 启动命令示例（带 JVM 参数）

```bash
# 示例：2GB 堆、G1 GC，便于生产观察
java -Xms1g -Xmx2g -XX:+UseG1GC -jar web-0.0.1-SNAPSHOT.jar
```

可根据实际内存将 `-Xms`/`-Xmx` 调大（如 4GB 机器用 `-Xmx2g`，8GB 用 `-Xmx4g`），再结合压测结果微调。


8核+64+2TB