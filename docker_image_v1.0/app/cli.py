#!/usr/bin/env python
"""
ARG-BiLSTM 命令行工具

用法:
    python cli.py end-to-end <input.fasta> <output_dir> <model_dir> [--threshold 0.5]
    
示例:
    # 使用Docker
    docker run -v /data:/data arg-bilstm end-to-end /data/input.fasta /data/output /app/models
    
    # 本地运行
    python cli.py end-to-end ./test.fasta ./results ./models --threshold 0.5
"""

import argparse
import os
import sys
import pandas as pd
from Bio import SeqIO
from reasoning import ARGPredictor


def run_end_to_end(input_file, output_dir, model_dir, threshold=0.5):
    """
    端到端预测流程
    
    Args:
        input_file: 输入FASTA文件路径
        output_dir: 输出目录路径
        model_dir: 模型文件目录路径
        threshold: 二分类阈值
    """
    # 1. 检查输入
    if not os.path.exists(input_file):
        print(f"[ERROR] 输入文件不存在: {input_file}")
        sys.exit(1)
    
    if not os.path.exists(model_dir):
        print(f"[ERROR] 模型目录不存在: {model_dir}")
        sys.exit(1)
    
    # 2. 创建输出目录
    os.makedirs(output_dir, exist_ok=True)
    
    # 3. 初始化预测器
    print(f"[1/4] 加载模型: {model_dir}")
    try:
        predictor = ARGPredictor(model_dir=model_dir)
    except Exception as e:
        print(f"[ERROR] 模型加载失败: {e}")
        sys.exit(1)
    
    # 4. 统计输入
    print(f"[2/4] 读取序列: {input_file}")
    records = list(SeqIO.parse(input_file, "fasta"))
    total_seqs = len(records)
    print(f"       共 {total_seqs} 条序列")
    
    if total_seqs == 0:
        print("[WARNING] 输入文件为空")
        return
    
    # 5. 批量预测
    print(f"[3/4] 开始预测 (threshold={threshold})")
    results = predictor.process_fasta_file(input_file, threshold=threshold, batch_size=256)
    
    # 6. 统计结果
    arg_count = sum(1 for r in results if r['is_arg'])
    non_arg_count = total_seqs - arg_count
    
    print(f"       - ARG: {arg_count}")
    print(f"       - Non-ARG: {non_arg_count}")
    
    # 7. 保存结果
    print(f"[4/4] 保存结果: {output_dir}")
    
    # 7.1 全部预测结果 (TSV)
    df_all = pd.DataFrame(results)
    df_all.to_csv(os.path.join(output_dir, "all_predictions.tsv"), sep='\t', index=False)
    
    # 7.2 仅ARG结果 (TSV)
    df_arg = df_all[df_all['is_arg'] == True]
    df_arg.to_csv(os.path.join(output_dir, "arg_predictions.tsv"), sep='\t', index=False)
    
    # 7.3 ARG序列 (FASTA)
    arg_ids = set(df_arg['id'].tolist())
    arg_records = [r for r in records if r.id in arg_ids]
    if arg_records:
        # 添加预测信息到description
        id_to_result = {r['id']: r for r in results if r['is_arg']}
        for record in arg_records:
            res = id_to_result[record.id]
            record.description = f"{record.id} | ARG_class={res['arg_class']} | class_prob={res['class_prob']} | binary_prob={res['binary_prob']}"
        
        SeqIO.write(arg_records, os.path.join(output_dir, "arg_sequences.fasta"), "fasta")
    
    # 7.4 类别统计
    if not df_arg.empty:
        class_counts = df_arg['arg_class'].value_counts()
        class_counts.to_csv(os.path.join(output_dir, "class_summary.tsv"), sep='\t', header=['count'])
        
        print("\n" + "=" * 50)
        print("ARG 类别分布:")
        print("=" * 50)
        for cls, count in class_counts.items():
            print(f"  {cls}: {count}")
        print("=" * 50)
    
    print(f"\n[完成] 结果已保存到: {output_dir}")
    print(f"  - all_predictions.tsv   (所有序列预测结果)")
    print(f"  - arg_predictions.tsv   (仅ARG序列)")
    print(f"  - arg_sequences.fasta   (ARG序列FASTA)")
    print(f"  - class_summary.tsv     (类别统计)")


def main():
    parser = argparse.ArgumentParser(
        description="ARG-BiLSTM: 抗性基因识别与分类工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # Docker运行
  docker run -v /data:/data arg-bilstm end-to-end /data/input.fasta /data/output /app/models
  
  # 本地运行
  python cli.py end-to-end ./input.fasta ./output ./models --threshold 0.5
        """
    )
    
    subparsers = parser.add_subparsers(dest="command", help="执行模式")
    
    # end-to-end 命令
    e2e_parser = subparsers.add_parser(
        "end-to-end", 
        help="完整预测流程: 二分类识别 + 多分类分类"
    )
    e2e_parser.add_argument("input", help="输入FASTA文件路径")
    e2e_parser.add_argument("output", help="输出目录路径")
    e2e_parser.add_argument("db", help="模型数据库目录路径")
    e2e_parser.add_argument(
        "--threshold", "-t",
        type=float, 
        default=0.5,
        help="二分类阈值 (默认: 0.5)"
    )
    
    args = parser.parse_args()
    
    if args.command == "end-to-end":
        run_end_to_end(args.input, args.output, args.db, args.threshold)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()

