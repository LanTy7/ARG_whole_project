# ARG 识别与分类工作流

用于大规模 MAG 数据库的抗性基因识别与分类

## 特性

- ⚡ **高性能**: GPU批量推理 + 多进程文件读取
- 🔄 **断点续传**: 支持中断后恢复
- 📊 **进度追踪**: 实时显示处理进度和速度
- 💾 **增量写入**: 边处理边写入，内存占用低

## 使用前准备

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 修改模型路径

编辑 `run_arg_analysis.py` 中的 CONFIG 部分：

```python
CONFIG = {
    'binary_model': '/path/to/binary/well-trained/bilstm_xxx.pth',
    'multi_model': '/path/to/multi/well-trained/bilstm_multi_xxx.pth',
    ...
}
```

或者通过命令行参数指定。

## 使用方法

### 基本用法

```bash
python run_arg_analysis.py \
    --input_dir /path/to/mag_database \
    --output arg_results.tsv
```

### 完整参数

```bash
python run_arg_analysis.py \
    --input_dir /path/to/mag_database \
    --output arg_results.tsv \
    --binary_model /path/to/binary_model.pth \
    --multi_model /path/to/multi_model.pth \
    --threshold 0.5 \
    --batch_size 2048 \
    --num_workers 8 \
    --file_batch 100
```

### 参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `--input_dir` | (必需) | 输入文件夹，包含 .faa 文件 |
| `--output` | `arg_results.tsv` | 输出 TSV 文件 |
| `--binary_model` | CONFIG中设置 | 二分类模型路径 |
| `--multi_model` | CONFIG中设置 | 多分类模型路径 |
| `--threshold` | 0.5 | 二分类阈值 |
| `--batch_size` | 2048 | GPU推理批次大小 |
| `--num_workers` | 8 | 文件读取并行数 |
| `--file_batch` | 100 | 每批处理文件数 |
| `--resume` | False | 从检查点恢复 |

### 断点续传

如果处理中断，使用 `--resume` 继续：

```bash
python run_arg_analysis.py \
    --input_dir /path/to/mag_database \
    --output arg_results.tsv \
    --resume
```

## 输出格式

输出 TSV 文件包含以下列：

| 列名 | 说明 |
|------|------|
| FileName | 来源文件名 |
| SequenceID | 序列ID |
| IsARG | 是否为ARG (True/False) |
| BinaryProb | 二分类概率 |
| ARGClass | ARG类别 (仅ARG有值) |
| ClassProb | 分类概率 (仅ARG有值) |

## 性能优化建议

### 1. 根据显存调整 batch_size

| 显存 | 建议 batch_size |
|------|-----------------|
| 8 GB | 1024 |
| 16 GB | 2048 |
| 24 GB | 4096 |
| 32 GB+ | 8192 |

### 2. 提交到集群

使用 SLURM 提交：

```bash
#!/bin/bash
#SBATCH --job-name=arg_analysis
#SBATCH --gpus=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=64G
#SBATCH --time=48:00:00
#SBATCH --output=arg_%j.log

module load cuda/11.8
module load python/3.10

python run_arg_analysis.py \
    --input_dir /data/mag_database \
    --output /results/arg_results.tsv \
    --batch_size 4096 \
    --num_workers 16
```

### 3. 使用 nohup 后台运行

```bash
nohup python run_arg_analysis.py \
    --input_dir /data/mag_database \
    --output arg_results.tsv \
    > analysis.log 2>&1 &
```

## 预估时间

以 3.4 亿条序列为例：

| 显卡 | batch_size | 预估时间 |
|------|------------|----------|
| V100 (16GB) | 2048 | ~8-12 小时 |
| A100 (40GB) | 8192 | ~3-5 小时 |

实际时间取决于序列长度分布和 I/O 速度。

## 日志文件

运行时会生成：
- `arg_analysis.log`: 详细日志
- `arg_results.tsv.checkpoint`: 检查点文件 (完成后可删除)

## 常见问题

**Q: 显存不足 (CUDA out of memory)?**
A: 降低 `--batch_size`

**Q: 文件读取慢?**
A: 增加 `--num_workers`，确保数据在快速存储上 (SSD)

**Q: 如何只输出ARG结果?**
A: 处理完成后过滤：
```bash
awk -F'\t' 'NR==1 || $3=="True"' arg_results.tsv > arg_only.tsv
```

