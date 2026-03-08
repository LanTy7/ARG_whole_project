# AGENTS.md

本仓库是实习项目"基于深度学习（BiLSTM）的全长抗性基因（ARG）预测（二分类）和分类（多分类）方法研究与实现"的代码与数据集合。

## 项目概述

本项目是一个完整的抗性基因（Antibiotic Resistance Gene, ARG）识别与分类系统，包含：
- 基于 BiLSTM 的深度学习模型训练和推理
- Web 可视化平台（前后端分离架构）
- Docker 容器化部署方案
- 大规模 MAG（宏基因组组装基因组）批处理工作流
- BLAST 序列比对功能

### 核心功能流程

1. **单序列/文件上传分析**：前端上传 FASTA 序列 → 后端调用 Docker 推理 → 返回预测结果 → 可视化展示
2. **MAG 批量分析**：上传 MAG 文件夹 → Prodigal 基因预测 → 合并蛋白质序列 → ARG 识别 → 结果落库
3. **BLAST 比对**：对预测出的 ARG 序列进行数据库比对，验证结果可靠性

## 项目结构

```
.
├── arg_visualization_web/     # Web 可视化平台
│   ├── backend/               # Spring Boot 后端 (Java 17)
│   └── frontend/              # Vue 3 前端
├── docker_image_v1.0/         # ARG-BiLSTM 推理 Docker 镜像
├── docker_image_prodigal/     # Prodigal 基因预测 Docker 镜像
├── workflow/                  # 大规模 MAG 批处理脚本
├── binary/                    # 二分类模型训练/测试（Jupyter Notebook）
├── multi/                     # 多分类模型训练/测试（Jupyter Notebook）
├── blast_db/                  # BLAST 数据库
│   ├── raw/                   # 原始数据
│   └── db/                    # 构建好的数据库文件（ARGNet_DB）
└── magdb/                     # MAG 测试数据
    ├── fasta_data/            # 原始 FASTA 文件
    └── prodigal_result/       # Prodigal 处理结果
```

## 技术栈

### 后端（arg_visualization_web/backend/）

- **框架**: Spring Boot 3.2.3
- **语言**: Java 17
- **构建工具**: Maven (pom.xml)
- **数据库**: MySQL + MyBatis Plus 3.5.5
- **缓存**: Redis
- **安全**: Spring Security + JWT (jjwt 0.11.5)
- **邮件**: Spring Mail
- **依赖管理**: 阿里云 Maven 仓库

### 前端（arg_visualization_web/frontend/）

- **框架**: Vue 3.5.22
- **构建工具**: Vite 6.4.1
- **UI 组件库**: Element Plus 2.8.8
- **状态管理**: Pinia 2.2.6
- **路由**: Vue Router 4.4.5
- **图表**: ECharts 5.5.1
- **国际化**: Vue I18n 9.14.5
- **HTTP 客户端**: Axios 1.7.7

### 深度学习推理（docker_image_v1.0/）

- **语言**: Python 3.10
- **深度学习**: PyTorch ≥2.0.0
- **生物信息学**: Biopython ≥1.80
- **数据处理**: NumPy, Pandas
- **容器化**: Docker (支持 CPU/GPU 版本)

### 批处理工作流（workflow/）

- Python 3.10+
- PyTorch
- Biopython
- Pandas, NumPy, tqdm
- 支持 GPU 批量推理和断点续传

## 代码组织

### 后端代码结构（Spring Boot）

```
backend/src/main/java/com/sy/
├── WebApplication.java              # 应用入口
├── config/                          # 配置类
│   ├── I18nConfig.java             # 国际化配置
│   ├── JwtConfig.java              # JWT配置
│   ├── MybatisPlusConfig.java      # MyBatis Plus配置
│   ├── RedisConfig.java            # Redis配置
│   ├── SecurityConfig.java         # 安全配置
│   └── WebMvcConfig.java           # Web MVC配置
├── controller/                      # 控制器层（REST API）
│   ├── AnalysisTaskController.java  # 分析任务管理
│   ├── AuthController.java         # 认证授权
│   ├── BlastController.java        # BLAST比对
│   ├── DownloadController.java     # 文件下载
│   ├── GenomeFileController.java   # 基因组文件管理
│   ├── MagController.java          # MAG分析
│   └── VisualizationController.java # 结果可视化
│       - `/genome/{taskId}` - 获取可视化数据
│       - `/genome/{taskId}/results` - 分页查询预测结果
│       - `/genome/{taskId}/class-summary` - 获取类别统计
│       - `/genome/{taskId}/arg-sequences` - 获取所有 ARG 序列（网络图专用）
├── service/                         # 服务接口层
│   ├── DockerService.java          # Docker调用接口
│   ├── MagAnalysisService.java     # MAG分析接口
│   ├── ProdigalService.java        # Prodigal接口
│   └── ...
├── service/impl/                    # 服务实现层
│   ├── DockerServiceImpl.java      # Docker调用实现（ARG推理）
│   ├── MagAnalysisServiceImpl.java # MAG分析编排
│   ├── ProdigalServiceImpl.java    # Prodigal并行处理
│   ├── BlastServiceImpl.java       # BLAST比对实现
│   └── ...
├── mapper/                          # 数据访问层（MyBatis）
├── pojo/                            # 实体类
├── vo/                              # 视图对象
├── util/                            # 工具类
│   ├── JwtUtil.java                # JWT工具
│   ├── EmailValidator.java         # 邮箱验证
│   └── ...
└── interceptor/                     # 拦截器
    ├── JwtInterceptor.java         # JWT认证拦截
    └── AdminAuthInterceptor.java   # 管理员权限拦截
```

### 前端代码结构（Vue 3）

```
frontend/src/
├── main.js                          # 应用入口
├── App.vue                          # 根组件
├── api/                             # API 接口封装
│   ├── auth.js                     # 认证相关
│   ├── task.js                     # 任务管理
│   ├── mag.js                      # MAG分析
│   ├── blast.js                    # BLAST比对
│   └── visualization.js            # 可视化数据获取（含网络图专用接口）
├── views/                           # 页面视图
│   ├── Login.vue                   # 登录页
│   ├── Home.vue                    # 首页
│   ├── Upload.vue                  # 文件上传
│   ├── Visualization.vue           # 结果可视化（含网络图、饼图、柱状图）
│   │   ├── 详情标签页 - 表格展示预测结果（支持分页）
│   │   └── 图表标签页 - ECharts 可视化
│   │       ├── 饼图 - ARG/非ARG分布
│   │       ├── 柱状图 - ARG 类别统计
│   │       └── 网络图 - ARG 类别与序列关系图（智能采样）
│   ├── History.vue                 # 历史记录
│   └── Admin.vue                   # 管理后台
├── components/                      # 公共组件
├── stores/                          # Pinia 状态管理
│   └── user.js                     # 用户状态
├── router/                          # 路由配置
│   └── index.js                    # 路由定义
├── locales/                         # 国际化
│   ├── zh.ts                       # 中文
│   └── en.ts                       # 英文
└── utils/                           # 工具函数
```

### 推理模块代码结构

```
docker_image_v1.0/
├── app/
│   ├── cli.py                      # 命令行入口
│   ├── model_definition.py         # BiLSTM 模型定义
│   └── reasoning.py                # 推理逻辑（ARGPredictor）
├── models/                         # 模型文件（需手动放入）
│   ├── binary_model.pth            # 二分类模型
│   └── multi_model.pth             # 多分类模型
└── Dockerfile / Dockerfile.gpu     # 容器构建文件
```

## 关键配置文件

### 后端配置（application.yml）

```yaml
server:
  port: 8080                        # 服务端口

spring:
  servlet:
    multipart:                      # 文件上传配置
      max-file-size: 524288000      # 最大 500MB
  data:
    redis:                          # Redis 配置
      host: ${REDIS_HOST:127.0.0.1}
  mail:                            # 邮件服务器配置
    host: smtp.qq.com

jwt:                               # JWT 配置
  secret: ${JWT_SECRET_KEY:...}
  expiration: 86400000             # 24小时

file.upload:                       # 文件上传路径
  genome-dir: /home/www/arg/uploads/genome
  mag-dir: /home/www/arg/uploads/genome

docker:                            # Docker 配置
  enabled: true
  arg:                             # ARG 推理镜像配置
    image-name: arg-bilstm:gpu
    use-gpu: true
  prodigal:                        # Prodigal 配置
    image-name: prodigal:latest
    parallel-threads: 8

queue:                             # 任务队列配置
  max-concurrent: 7                # 最大并发任务数

blast:                             # BLAST 配置
  enabled: true
  db-path: /home/www/arg/blast_db/db/ARGNet_DB
```

### 前端配置（vite.config.js）

```javascript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  # 后端 API 地址
        changeOrigin: true
      }
    }
  }
})
```

## 构建和运行

### 后端构建

```bash
cd arg_visualization_web/backend/
mvn clean install
mvn spring-boot:run
```

### 前端构建

```bash
cd arg_visualization_web/frontend/
npm install    # 首次运行
npm run dev    # 开发模式
npm run build  # 生产构建
```

### Docker 镜像构建

```bash
# ARG-BiLSTM 推理镜像（CPU 版本）
cd docker_image_v1.0/
docker build -t arg-bilstm:latest .

# ARG-BiLSTM 推理镜像（GPU 版本）
docker build -f Dockerfile.gpu -t arg-bilstm:gpu .

# Prodigal 镜像
cd docker_image_prodigal/
docker build -t prodigal:latest .
```

### 模型准备

项目不包含预训练模型文件，需手动放入：

```bash
# 从 binary/model_train/ 和 multi/model_train/ 训练结果复制
cp /path/to/binary/well-trained/bilstm_xxx.pth docker_image_v1.0/models/binary_model.pth
cp /path/to/multi/well-trained/bilstm_multi_xxx.pth docker_image_v1.0/models/multi_model.pth
```

## 核心业务流程

### 1. 单文件分析流程

```
用户上传 FASTA → GenomeFileController → 保存文件 → 
AnalysisTaskController 创建任务 → TaskQueueManager 排队 → 
DockerServiceImpl 调用 arg-bilstm 容器 → 解析 TSV 结果 → 
VisualizationService 落库 → 前端轮询状态 → 展示结果
```

### 2. MAG 批量分析流程

```
用户上传 MAG 文件夹 → MagController → 创建任务 → 
MagAnalysisServiceImpl 编排：
  1. ProdigalServiceImpl 并行处理 FASTA → .faa
  2. 合并所有 .faa 文件
  3. DockerServiceImpl 调用 arg-bilstm 分析
→ 结果保存 → 任务完成落库
```

### 3. BLAST 比对流程

```
前端请求 BLAST → BlastController → BlastServiceImpl →
提取序列 → 运行 ncbi/blast 容器 → 解析比对结果 → 返回
```

### 4. ARG 网络图可视化流程

**数据流**：
- **DB 模式（数据已落库）**：前端获取分页数据 → 切换到图表页时调用 `getAllArgSequences` 获取全部 ARG 序列 → 智能采样渲染网络图
- **文件模式**：直接解析 TSV 文件 → 前端处理全部数据 → 渲染网络图

**智能采样策略**：
- 总序列 > 1000：每类显示前 20 个概率最高的序列
- 总序列 > 500：每类显示前 30 个
- 总序列 > 200：每类显示前 40 个
- 其他：每类显示前 50 个
- 类别节点显示 `(显示数/总数)`，如 `氨基糖苷类 (20/156)`

**新增后端接口**：
- `GET /api/visualization/genome/{taskId}/arg-sequences` - 获取所有 ARG 序列（用于网络图）

## 开发约定

### 代码风格

- **后端**: 遵循 Spring Boot 惯例，使用 Lombok 简化代码，接口返回统一 Result 对象
- **前端**: Vue 3 Composition API 风格，使用 Pinia 管理状态，axios 封装请求
- **Python**: 遵循 PEP 8，使用类型注解，详细的 docstring

### 可视化图表规范

**网络图（ARG 关系图）**：
- **数据来源**：DB 模式下通过 `getAllArgSequences` 接口获取全部 ARG 序列
- **智能采样**：节点数过多时自动采样，每类别最多显示前 50 个（按概率排序）
- **初始布局**：力引导布局（force layout），使用 `circular` 初始分布
- **交互控制**：支持滚轮缩放（0.05x ~ 5x）、拖拽平移、点击节点触发 BLAST
- **性能优化**：总节点数控制在 200-500 以内，确保渲染流畅
- **中文支持**：所有标签、提示使用中文，类别名映射（Unknown → 未知类别）

**柱状图/饼图**：
- 使用 ECharts 5.x 版本
- 统一使用青绿色系配色方案
- 支持响应式布局和窗口大小变化自适应

### 异常处理

- 后端使用 `@ControllerAdvice` 全局异常处理
- 业务异常统一封装为 RuntimeException
- 前端统一拦截处理 HTTP 错误

### 日志规范

- 后端使用 SLF4J + Lombok `@Slf4j`
- 关键业务流程必须记录 INFO 级别日志
- 异常情况必须记录 ERROR 级别日志并带堆栈

### 数据库设计

主要表结构：
- `user` - 用户信息
- `genome_file` - 上传的基因组文件
- `analysis_task` - 分析任务
- `all_prediction` - 预测结果明细
- `class_summary` - 类别统计汇总
- `login_log` - 登录日志

## 测试策略

- 模型训练和测试使用 Jupyter Notebook（binary/, multi/ 目录）
- 单元测试：Spring Boot Test + MyBatis Plus Test
- 集成测试：通过 Docker Compose 启动完整环境测试

## 安全考虑

1. **认证授权**: JWT Token + Spring Security，支持 Token 过期和刷新
2. **权限控制**: 基于角色的访问控制（USER/ADMIN）
3. **文件上传**: 限制文件大小（500MB），校验文件类型
4. **SQL 注入**: 使用 MyBatis Plus 参数化查询
5. **XSS 防护**: 前端输入校验，后端输出转义
6. **敏感配置**: 使用环境变量或外部配置（application.yml 中 ${} 占位符）

## 部署注意事项

1. **依赖服务**: MySQL、Redis、Docker 必须预先安装
2. **目录权限**: 确保文件上传目录和输出目录有写权限
3. **Docker 访问**: 后端需要访问 Docker Socket 或 Docker CLI
4. **GPU 支持**: GPU 版本需要 NVIDIA Docker 和 CUDA 驱动
5. **内存配置**: 大文件分析建议 JVM 堆内存 ≥4G

## 常用命令

```bash
# 启动完整开发环境
# 1. 启动 MySQL 和 Redis
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=xxx mysql:8
docker run -d -p 6379:6379 redis:latest

# 2. 启动后端
cd arg_visualization_web/backend && mvn spring-boot:run

# 3. 启动前端
cd arg_visualization_web/frontend && npm run dev

# 运行批量分析
python workflow/run_arg_analysis.py \
    --input_dir /path/to/mag_database \
    --output results.tsv \
    --batch_size 2048
```
