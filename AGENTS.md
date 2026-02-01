# AGENTS.md

本仓库是实习项目“基于深度学习（BiLSTM）的全长抗性基因（ARG）预测（二分类）和分类（多分类）方法研究与实现”的代码与数据集合，包含模型训练/推理、可视化网站、批量分析流程、BLAST 数据库等内容。

## 总体结构

- `arg_visualization_web/`：可视化网站（前后端）
  - `backend/`：Spring Boot 3.2（Java 17），MyBatis/MySQL/Redis/Security 等依赖（`pom.xml`）
  - `frontend/`：Vue 3 + Vite + Element Plus + ECharts（`package.json`）
- `docker_image_v1.0/`：ARG-BiLSTM 推理镜像（`app/` 推理代码；`models/` 需手动放入模型）
- `docker_image_prodigal/`：Prodigal 预处理镜像（MAG → 蛋白质序列）
- `workflow/`：大规模 MAG 批处理脚本（`run_arg_analysis.py`，见 `workflow/README.md`）
- `binary/`：二分类模型训练/测试笔记本
- `multi/`：多分类模型训练/测试笔记本
- `blast_db/`：BLAST 数据库（`raw/` 原始数据，`db/` 生成库）
- `magdb/`：MAG 测试数据（原始 fasta 与 prodigal 结果）
- `latex/`：演示文稿 LaTeX 模板与任务书（`main.tex`, `smile_styles.tex`, `references.bib`, `task book.md`）

## 关键流程（高层）

1. 前端上传序列 / fasta / magdb
2. 后端调用推理镜像（`docker_image_v1.0`），必要时先用 Prodigal 处理 MAG（`docker_image_prodigal`）
3. 返回预测与分类结果，前端可视化展示并支持下载与 BLAST 查询

## 运行/构建提示

- 推理镜像与批量脚本依赖训练好的模型文件（`*.pth`），默认不在仓库中，需要自行放入：
  - `docker_image_v1.0/models/binary_model.pth`
  - `docker_image_v1.0/models/multi_model.pth`
- 具体使用方式参考：
  - `docker_image_v1.0/README.md`
  - `docker_image_prodigal/README.md`
  - `workflow/README.md`

## 协作约定

- 大文件（模型、数据库、MAG 数据）不要在不必要时改动或重写。
- 前端已有 `node_modules/`，避免无意义的重新安装或清理。
- 进行演示/汇报材料制作时，保持项目结构不变，将新内容放到新增目录中。
