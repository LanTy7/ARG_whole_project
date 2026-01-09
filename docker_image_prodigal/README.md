# Prodigal Docker Image

用于 MAG (宏基因组组装基因组) 基因预测的 Docker 镜像。

## 功能

将原始核酸序列（.fa/.fasta）转换为蛋白质序列（.faa），供 ARG 识别模型使用。

## 构建镜像

```bash
cd docker_image_prodigal
docker build -t prodigal:latest .
```

## 使用方法

### 基本用法

```bash
docker run --rm \
  -v /path/to/input:/input:ro \
  -v /path/to/output:/output \
  prodigal:latest \
  -i /input/genome.fa \
  -a /output/proteins.faa \
  -o /output/genes.gff \
  -f gff
```

### 参数说明

| 参数 | 说明 |
|------|------|
| `-i` | 输入文件（核酸序列，FASTA格式） |
| `-a` | 输出蛋白质序列文件（.faa） |
| `-o` | 输出基因注释文件（.gff） |
| `-f gff` | 输出格式为 GFF |
| `-p meta` | 使用宏基因组模式（可选，对于短序列更好） |

### 示例

```bash
# 处理单个文件
docker run --rm \
  -v /data/mag:/input:ro \
  -v /data/output:/output \
  prodigal:latest \
  -i /input/SRR15910072.bin_21.fa \
  -a /output/SRR15910072.bin_21.faa \
  -o /output/SRR15910072.bin_21.gff \
  -f gff \
  -p meta
```

## 输出文件

| 文件 | 说明 |
|------|------|
| `*.faa` | 蛋白质序列文件（用于 ARG 分析） |
| `*.gff` | 基因注释文件（GFF格式） |

## 在 ARG 分析流程中的位置

```
MAG 文件 (.fa) → Prodigal → 蛋白质序列 (.faa) → ARG-BiLSTM → 预测结果
```
