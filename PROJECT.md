# PROJECT.md

本文档为项目说明文档，为团队成员和AI助手提供项目概述和技术细节。

## 项目概述

本项目是一个**抗生素抗性基因（ARG）识别系统**，基于BiLSTM深度学习模型构建，主要功能包括：

- **二分类识别**：判断序列是否为ARG（抗性基因）
- **多分类分类**：将ARG分为14种类型（MLS、氨基糖苷类、β-内酰胺、四环素等）
- **Web可视化界面**：Vue 3前端 + Spring Boot后端
- **BLAST序列相似性搜索**：支持对预测结果进行验证
- **MAG批量分析**：支持宏基因组组装基因组的大规模分析

## 技术栈

- **后端框架**：Spring Boot 3.2.3（Java 17）、MyBatis Plus 3.5.5、MySQL、Redis、Spring Security + JWT
- **前端框架**：Vue 3（Composition API）、Vite 6、Element Plus 2.8.8、Pinia 2.2.6、ECharts 5.5.1
- **机器学习**：PyTorch BiLSTM模型（Docker容器化部署）
- **生物信息学工具**：Prodigal（基因预测）、NCBI BLAST（序列相似性搜索）
- **国际化**：支持中文（zh）和英文（en）界面切换

## 项目结构

```
ARG_whole_project/
├── arg_visualization_web/         # 可视化网站（前后端）
│   ├── backend/                   # Spring Boot后端
│   └── frontend/                  # Vue 3前端
├── docker_image_v1.0/             # ARG-BiLSTM推理Docker镜像
├── docker_image_prodigal/         # Prodigal预处理Docker镜像
├── workflow/                      # 大规模MAG批处理脚本
├── binary/                        # 二分类模型训练（Jupyter Notebook）
├── multi/                         # 多分类模型训练（Jupyter Notebook）
├── blast_db/                      # BLAST数据库
├── magdb/                         # MAG测试数据
└── AGENTS.md / CLAUDE.md          # 项目说明文档
```

## 构建与运行命令

### 后端构建与运行

```bash
cd arg_visualization_web/backend
# Maven构建
mvn clean package -DskipTests
# 运行Spring Boot应用
java -jar target/web-0.0.1-SNAPSHOT.jar
```

后端默认端口：8080

### 前端构建与运行

```bash
cd arg_visualization_web/frontend
# 开发模式启动
npm run dev
# 生产构建
npm run build
```

### Docker镜像构建

```bash
# 构建ARG-BiLSTM推理镜像（CPU版本）
cd docker_image_v1.0 && docker build -t arg-bilstm:latest .

# 构建ARG-BiLSTM推理镜像（GPU版本）
cd docker_image_v1.0 && docker build -f Dockerfile.gpu -t arg-bilstm:gpu .

# 构建Prodigal预处理镜像
cd docker_image_prodigal && docker build -t prodigal:latest .
```

### 批量分析工作流

```bash
cd workflow
# 安装依赖
pip install -r requirements.txt
# 运行批量分析
python run_arg_analysis.py --input_dir /path/to/mags --output results.tsv
```

## 系统架构

### 数据流程

```
用户上传 → 后端存储 → Docker容器（Prodigal预处理 → ARG-BiLSTM推理） → 结果存储 → 前端可视化
```

### API接口结构

| 接口路径 | 功能描述 |
|---------|---------|
| `/api/auth` | 用户认证（登录、注册、JWT令牌管理） |
| `/api/analysis` | 分析任务管理（创建、查询、取消任务） |
| `/api/file` | 文件操作（上传、下载、删除） |
| `/api/mag` | MAG分析（宏基因组组装基因组分析） |
| `/api/visualization` | 可视化数据（图表、统计信息） |
| `/api/blast` | BLAST搜索（序列相似性比对） |
| `/api/admin` | 管理员功能（用户管理、系统监控） |
| `/api/download` | 结果下载（预测结果、序列文件） |

### 后端关键文件

| 文件路径 | 用途说明 |
|---------|---------|
| `application.yml` | 应用配置（数据库、Redis、Docker、文件上传、BLAST） |
| `messages_*.properties` | 国际化资源文件（中文、英文） |
| `pom.xml` | Maven依赖配置（Spring Boot、MyBatis Plus、Security等） |
| `src/main/java/com/sy/controller/` | REST API控制器 |
| `src/main/java/com/sy/service/` | 业务逻辑层（服务接口与实现） |
| `src/main/java/com/sy/mapper/` | MyBatis数据访问层 |
| `src/main/java/com/sy/pojo/` | 实体类（User、AnalysisTask、AnalysisResult等） |
| `src/main/java/com/sy/util/` | 工具类（JWT、密码编码、国际化等） |

### 前端关键文件

| 文件路径 | 用途说明 |
|---------|---------|
| `src/views/` | 页面组件（Login、Upload、Visualization、History等） |
| `src/api/` | API接口定义（Axios请求封装） |
| `src/router/index.js` | Vue Router配置（路由守卫、懒加载） |
| `src/stores/user.js` | Pinia用户状态管理 |
| `src/utils/request.js` | Axios请求封装（JWT拦截器、错误处理） |
| `src/locales/` | 前端国际化文件 |
| `package.json` | npm依赖配置（Vue 3、Vite、Element Plus等） |

## 外部依赖（需手动提供）

以下文件不在仓库中，需要单独获取并放置到指定位置：

| 文件路径 | 用途说明 |
|---------|---------|
| `docker_image_v1.0/models/binary_model.pth` | 二分类PyTorch模型文件 |
| `docker_image_v1.0/models/multi_model.pth` | 多分类PyTorch模型文件 |
| `blast_db/db/ARGNet_DB*` | BLAST数据库文件（已预构建） |

## 开发模式与约定

### 后端开发规范

- **架构模式**：采用Service Layer模式，业务逻辑集中在service层
- **响应格式**：所有API统一使用`Result<T>`包装类
  ```json
  {
    "code": 0,
    "message": "操作成功",
    "data": {...}
  }
  ```
- **国际化**：使用`I18nUtil`工具类进行后端消息国际化
- **认证授权**：基于Spring Security + JWT的无状态认证
- **异常处理**：全局异常处理器统一处理业务异常和系统异常

### 前端开发规范

- **组件风格**：使用Vue 3 Composition API（`<script setup>`语法）
- **路由策略**：采用懒加载（lazy-loaded routes）优化首屏加载
- **HTTP请求**：使用Axios封装请求，集成JWT令牌自动刷新
- **状态管理**：使用Pinia进行全局状态管理（用户信息、任务状态等）
- **UI组件**：基于Element Plus组件库，保持一致的视觉风格
- **数据可视化**：使用ECharts展示ARG分析结果和统计数据

### 协作约定

- **大文件处理**：模型文件、数据库文件、MAG测试数据等大文件不要随意修改
- **前端依赖**：项目已包含`node_modules/`，避免不必要的重新安装
- **演示材料**：制作演示或汇报材料时，将新内容放到新增目录，保持项目结构不变
- **代码风格**：遵循各技术栈的官方最佳实践，保持代码一致性

## 主要功能模块说明

### 用户认证模块

- **登录/注册**：支持用户注册和登录，生成JWT访问令牌
- **令牌管理**：访问令牌（Access Token）和刷新令牌（Refresh Token）机制
- **权限控制**：普通用户和管理员角色分离，路由守卫保护受限页面
- **安全防护**：密码加密存储、登录日志记录、IP定位

### 文件上传模块

- **支持格式**：FASTA格式（DNA、蛋白质序列）、MAG文件
- **文件存储**：上传文件存储在后端`uploads/`目录，按用户/任务组织
- **类型识别**：自动识别序列类型和文件格式

### 分析任务模块

- **任务创建**：用户提交分析请求，生成分析任务
- **任务队列**：使用任务队列管理分析请求，支持并发处理
- **进度追踪**：实时显示任务执行进度和状态
- **结果存储**：分析结果持久化到数据库，支持历史查询

### MAG分析模块

- **Prodigal预处理**：MAG核酸序列转换为蛋白质序列
- **批量处理**：支持大规模MAG数据库的批量分析
- **断点续传**：长时间任务支持检查点恢复
- **增量输出**：降低内存占用，支持超大文件处理

### 可视化模块

- **结果展示**：以表格和图表形式展示预测结果
- **统计图表**：ARG类型分布、概率分布等统计图表
- **序列浏览器**：支持查看和分析预测序列
- **交互操作**：支持筛选、排序、导出等功能

### BLAST搜索模块

- **序列比对**：将预测序列与ARG数据库进行相似性搜索
- **结果展示**：展示相似序列、E值、相似度等信息
- **注释验证**：支持对预测结果进行验证和注释

## 批量分析工作流参数说明

`run_arg_analysis.py`脚本支持以下主要参数：

| 参数 | 说明 | 默认值 |
|-----|------|-------|
| `--input_dir` | 输入目录（.faa蛋白质序列文件） | 必填 |
| `--output` | 输出TSV文件路径 | 必填 |
| `--binary_model` | 二分类模型路径 | `models/binary_model.pth` |
| `--multi_model` | 多分类模型路径 | `models/multi_model.pth` |
| `--threshold` | 二分类概率阈值 | 0.5 |
| `--batch_size` | GPU批处理大小 | 2048 |
| `--num_workers` | 并行文件读取数 | 8 |
| `--resume` | 从检查点恢复 | False |

输出TSV文件格式：

| 列名 | 说明 |
|-----|------|
| FileName | 来源文件名 |
| SequenceID | 序列ID |
| IsARG | 是否为ARG（True/False） |
| BinaryProb | 二分类概率 |
| ARGClass | ARG类别（仅ARG有值） |
| ClassProb | 分类概率（仅ARG有值） |

## 模型说明

### BiLSTM二分类模型

- **功能**：判断输入序列是否为抗生素抗性基因
- **输入**：蛋白质序列（氨基酸序列）
- **输出**：二分类结果（ARG/非ARG）及概率

### BiLSTM多分类模型

- **功能**：将ARG分为14种类型
- **输入**：蛋白质序列（氨基酸序列）
- **输出**：14种ARG类型的分类结果及对应概率

**14种ARG类型**：
1. MLS（大环内酯-林可酰胺-链阳菌素）
2. Aminoglycoside（氨基糖苷类）
3. Beta-lactam（β-内酰胺类）
4. Tetracycline（四环素类）
5. Chloramphenicol（氯霉素类）
6. Sulfonamide（磺胺类）
7. Trimethoprim（三甲氧苄氨嘧啶类）
8. Quinolone（喹诺酮类）
9. Fosfomycin（磷霉素类）
10. Glycopeptide（糖肽类）
11. Macrolide（大环内酯类）
12. Multidrug（多重耐药类）
13. Others（其他类）
14. Unclassified（未分类）

## 常见问题

### Q1：模型文件不存在怎么办？
需要从模型训练流程中导出训练好的`.pth`模型文件，放置到`docker_image_v1.0/models/`目录。

### Q2：如何切换中英文界面？
前端支持语言切换，可通过界面上的语言切换组件或浏览器语言偏好自动切换。

### Q3：如何添加新的ARG类型？
需要重新训练多分类模型，修改模型定义中的类别数量，并更新前端和后端的相应代码。

### Q4：任务队列如何管理？
使用Redis实现分布式任务队列，支持多实例部署和任务分发。

### Q5：如何进行BLAST搜索？
前端提供BLAST搜索界面，用户选择序列后自动提交搜索请求，后端调用BLAST工具进行比对。
