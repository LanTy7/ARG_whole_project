# ARG-BiLSTM Docker Image

基于 BiLSTM 的抗性基因(ARG)识别与分类工具。

## 功能

1. **二分类识别**: 判断蛋白质序列是否为抗性基因
2. **多分类分类**: 对识别出的ARG进行类型分类

## 目录结构

```
docker_image_v1.0/
├── app/
│   ├── cli.py              # 命令行入口
│   ├── model_definition.py # 模型定义
│   └── reasoning.py        # 推理逻辑
├── models/
│   ├── binary_model.pth    # 二分类模型 (需手动放入)
│   └── multi_model.pth     # 多分类模型 (需手动放入)
├── Dockerfile              # CPU版本
├── Dockerfile.gpu          # GPU版本
├── requirements.txt        # CPU版本依赖 (含PyTorch)
├── requirements-gpu.txt    # GPU版本依赖 (不含PyTorch，基础镜像已有)
└── README.md
```

## 使用前准备

### 1. 放入模型文件

将训练好的模型文件复制到 `models/` 目录:

```bash
cp /path/to/binary/well-trained/bilstm_xxx.pth ./models/binary_model.pth
cp /path/to/multi/well-trained/bilstm_multi_xxx.pth ./models/multi_model.pth
```

### 2. 构建 Docker 镜像

**CPU版本** (推荐，体积较小):
```bash
docker build -t arg-bilstm:latest .
```

**GPU版本**:
```bash
docker build -f Dockerfile.gpu -t arg-bilstm:gpu .
```

## 使用方法

### 基本用法

```bash
docker run -v /your/data:/data arg-bilstm end-to-end \
    /data/input.fasta \
    /data/output \
    /app/models
```

### 完整参数

```bash
docker run -v /your/data:/data arg-bilstm end-to-end \
    /data/input.fasta \      # 输入FASTA文件
    /data/output \           # 输出目录
    /app/models \            # 模型目录 (镜像内默认路径)
    --threshold 0.5          # 二分类阈值 (可选，默认0.5)
```

### GPU版本运行

```bash
docker run --gpus all -v /your/data:/data arg-bilstm:gpu end-to-end \
    /data/input.fasta \
    /data/output \
    /app/models
```

## 输出文件

运行后会在输出目录生成以下文件:

| 文件名 | 描述 |
|--------|------|
| `all_predictions.tsv` | 所有序列的预测结果 |
| `arg_predictions.tsv` | 仅ARG序列的预测结果 |
| `arg_sequences.fasta` | ARG序列FASTA文件 |
| `class_summary.tsv` | ARG类别统计 |

### 输出格式示例

`arg_predictions.tsv`:
```
id	is_arg	binary_prob	arg_class	class_prob
seq_001	True	0.9823	beta-lactam	0.9456
seq_002	True	0.8912	tetracycline	0.8234
```

## API调用 (Python)

如果需要在Python代码中调用:

```python
from reasoning import ARGPredictor

# 初始化
predictor = ARGPredictor(model_dir="./models")

# 单条预测
result = predictor.predict_sequence("seq_id", "MKTLLLTLVVVALLVQPAASA...")

# 批量预测
results = predictor.process_fasta_file("input.fasta", threshold=0.5)
```

## 模型信息

### 二分类模型
- 架构: BiLSTM + Global Max/Avg Pooling
- 输入: 氨基酸索引编码
- 输出: ARG概率 (0-1)

### 多分类模型  
- 架构: BiLSTM + Global Max/Avg Pooling
- 输入: One-hot编码
- 类别: MLS, aminoglycoside, bacitracin, beta-lactam, chloramphenicol, fosfomycin, glycopeptide, multidrug, polymyxin, quinolone, sulfonamide, tetracycline, trimethoprim, Others

## 性能指标

- 二分类: AUC > 0.95
- 多分类: Accuracy ≈ 85%

## 常见问题

**Q: 如何调整阈值?**
A: 使用 `--threshold` 参数。较高阈值(如0.7)会减少假阳性但可能漏检，较低阈值(如0.3)会提高召回率但增加假阳性。

**Q: GPU版本无法运行?**
A: 确保安装了 NVIDIA Docker 并使用 `--gpus all` 参数。

**Q: 如何使用外部模型文件?**
A: 挂载模型目录: `-v /path/to/models:/models`，然后指定 `/models` 作为模型目录参数。

