#!/bin/bash
#SBATCH --job-name=arg_analysis
#SBATCH --partition=gpu           # GPU分区 (根据集群修改)
#SBATCH --gpus=1                  # 申请1块GPU
#SBATCH --cpus-per-task=16        # CPU核心数
#SBATCH --mem=64G                 # 内存
#SBATCH --time=48:00:00           # 最大运行时间
#SBATCH --output=arg_%j.log       # 日志输出
#SBATCH --error=arg_%j.err        # 错误输出

# ============================================================
# 配置 (请根据实际情况修改)
# ============================================================

# 输入输出路径
INPUT_DIR="/path/to/mag_database"           # MAG数据库文件夹
OUTPUT_FILE="/path/to/output/arg_results.tsv"

# 模型路径
BINARY_MODEL="/path/to/binary/well-trained/bilstm_xxx.pth"
MULTI_MODEL="/path/to/multi/well-trained/bilstm_multi_xxx.pth"

# 推理参数 (根据显卡显存调整)
BATCH_SIZE=4096      # V100: 2048, A100: 8192
NUM_WORKERS=16       # 文件读取并行数
FILE_BATCH=200       # 每批处理文件数
THRESHOLD=0.5        # 二分类阈值

# ============================================================
# 环境配置
# ============================================================

# 加载模块 (根据集群修改)
# module load cuda/11.8
# module load python/3.10

# 激活虚拟环境 (如果有)
# source /path/to/venv/bin/activate

# 显示环境信息
echo "============================================"
echo "Job ID: $SLURM_JOB_ID"
echo "Node: $SLURM_NODELIST"
echo "GPU: $CUDA_VISIBLE_DEVICES"
echo "Time: $(date)"
echo "============================================"

# 显示GPU信息
nvidia-smi

# ============================================================
# 运行
# ============================================================

cd $(dirname $0)

python run_arg_analysis.py \
    --input_dir "$INPUT_DIR" \
    --output "$OUTPUT_FILE" \
    --binary_model "$BINARY_MODEL" \
    --multi_model "$MULTI_MODEL" \
    --threshold $THRESHOLD \
    --batch_size $BATCH_SIZE \
    --num_workers $NUM_WORKERS \
    --file_batch $FILE_BATCH

echo "============================================"
echo "Completed at: $(date)"
echo "============================================"

