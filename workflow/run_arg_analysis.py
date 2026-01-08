#!/usr/bin/env python
"""
ARG 识别与分类工作流

用于大规模 MAG 数据库的抗性基因识别与分类
支持断点续传、多进程文件读取、GPU 批量推理

用法:
    python run_arg_analysis.py --input_dir /path/to/faa_folder --output results.tsv
"""

import os
import sys
import argparse
import glob
import time
import logging
from datetime import datetime
from pathlib import Path
from concurrent.futures import ProcessPoolExecutor, as_completed
from collections import defaultdict

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from Bio import SeqIO
from tqdm import tqdm

# ============================================================
# 配置
# ============================================================

CONFIG = {
    # 模型路径 (请修改为实际路径)
    'binary_model': '/path/to/binary/well-trained/bilstm_xxx.pth',
    'multi_model': '/path/to/multi/well-trained/bilstm_multi_xxx.pth',
    
    # 推理参数
    'binary_threshold': 0.5,    # 二分类阈值
    'batch_size': 2048,         # GPU批次大小 (根据显存调整)
    'num_workers': 8,           # 文件读取并行数
    
    # 性能参数
    'file_batch_size': 100,     # 每次处理的文件数
    'checkpoint_interval': 1000, # 每处理多少文件保存一次检查点
}

# 日志配置
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('arg_analysis.log')
    ]
)
logger = logging.getLogger(__name__)


# ============================================================
# 模型定义 (必须与训练代码一致)
# ============================================================

class BiLSTMBinary(nn.Module):
    """二分类模型"""
    def __init__(self, config):
        super().__init__()
        self.embedding = nn.Embedding(
            config['vocab_size'], 
            config['embedding_dim'], 
            padding_idx=0
        )
        self.lstm = nn.LSTM(
            input_size=config['embedding_dim'],
            hidden_size=config['hidden_size'],
            num_layers=config['num_layers'],
            batch_first=True,
            bidirectional=True,
            dropout=config['dropout'] if config['num_layers'] > 1 else 0
        )
        self.dropout = nn.Dropout(config['dropout'])
        self.classifier = nn.Sequential(
            nn.Linear(config['hidden_size'] * 4, config['hidden_size']),
            nn.ReLU(),
            nn.Dropout(config['dropout']),
            nn.Linear(config['hidden_size'], 1)
        )

    def forward(self, x):
        emb = self.embedding(x)
        output, _ = self.lstm(emb)
        max_pool, _ = torch.max(output, dim=1)
        avg_pool = torch.mean(output, dim=1)
        features = torch.cat([max_pool, avg_pool], dim=1)
        return self.classifier(self.dropout(features))


class BiLSTMClassifier(nn.Module):
    """多分类模型"""
    def __init__(self, config, num_classes):
        super().__init__()
        self.lstm = nn.LSTM(
            input_size=config['embedding_size'],
            hidden_size=config['hidden_size'],
            num_layers=config['num_layers'],
            batch_first=True,
            bidirectional=True,
            dropout=config['dropout'] if config['num_layers'] > 1 else 0
        )
        self.dropout = nn.Dropout(config['dropout'])
        self.classifier = nn.Sequential(
            nn.Linear(config['hidden_size'] * 4, config['hidden_size']),
            nn.ReLU(),
            nn.Dropout(config['dropout']),
            nn.Linear(config['hidden_size'], num_classes)
        )

    def forward(self, x):
        output, _ = self.lstm(x)
        max_pool, _ = torch.max(output, dim=1)
        avg_pool = torch.mean(output, dim=1)
        features = torch.cat([max_pool, avg_pool], dim=1)
        return self.classifier(self.dropout(features))


# ============================================================
# 序列编码
# ============================================================

# 二分类编码
AMINO_ACIDS = 'ACDEFGHIKLMNPQRSTVWY'
AA_DICT_BINARY = {aa: i + 1 for i, aa in enumerate(AMINO_ACIDS)}
AA_DICT_BINARY.update({'X': 21, 'PAD': 0})

# 多分类编码
AA_DICT_MULTI = {aa: i for i, aa in enumerate(AMINO_ACIDS)}
AA_DICT_MULTI.update({
    'B': [AA_DICT_MULTI['D'], AA_DICT_MULTI['N']],
    'Z': [AA_DICT_MULTI['E'], AA_DICT_MULTI['Q']],
    'J': [AA_DICT_MULTI['I'], AA_DICT_MULTI['L']],
    'X': 'ANY',
    'PAD': 20
})


def encode_binary(sequence, max_length):
    """二分类编码: 序列 -> 索引"""
    indices = [AA_DICT_BINARY.get(aa, 21) for aa in sequence.upper()]
    indices = indices[:max_length]
    if len(indices) < max_length:
        indices += [0] * (max_length - len(indices))
    return np.array(indices, dtype=np.int64)


def encode_multi(sequence, max_length):
    """多分类编码: 序列 -> One-hot"""
    encoding = np.zeros((max_length, 21), dtype=np.float32)
    sequence = sequence.upper()
    for i in range(min(len(sequence), max_length)):
        aa = sequence[i]
        if aa in AA_DICT_MULTI:
            idx = AA_DICT_MULTI[aa]
            if isinstance(idx, list):
                for j in idx:
                    encoding[i, j] = 0.5
            elif idx == 'ANY':
                encoding[i, :20] = 0.05
            else:
                encoding[i, idx] = 1.0
        else:
            encoding[i, :20] = 0.05
    if len(sequence) < max_length:
        encoding[len(sequence):, 20] = 1.0
    return encoding


# ============================================================
# 文件读取 (多进程)
# ============================================================

def read_fasta_file(file_path):
    """读取单个FASTA文件"""
    try:
        records = []
        for record in SeqIO.parse(file_path, "fasta"):
            seq = str(record.seq).upper().replace('*', '').replace('-', '')
            if len(seq) > 0:
                records.append({
                    'file': os.path.basename(file_path),
                    'id': record.id,
                    'seq': seq
                })
        return records
    except Exception as e:
        logger.warning(f"Error reading {file_path}: {e}")
        return []


def load_files_parallel(file_list, num_workers=8):
    """并行读取多个文件"""
    all_records = []
    with ProcessPoolExecutor(max_workers=num_workers) as executor:
        futures = {executor.submit(read_fasta_file, f): f for f in file_list}
        for future in as_completed(futures):
            records = future.result()
            all_records.extend(records)
    return all_records


# ============================================================
# ARG 分析器
# ============================================================

class ARGAnalyzer:
    """ARG 识别与分类分析器"""
    
    def __init__(self, binary_model_path, multi_model_path, device='cuda'):
        self.device = torch.device(device if torch.cuda.is_available() else 'cpu')
        logger.info(f"Using device: {self.device}")
        
        # 加载二分类模型
        logger.info(f"Loading binary model: {binary_model_path}")
        binary_ckpt = torch.load(binary_model_path, map_location=self.device, weights_only=False)
        self.binary_config = binary_ckpt['config']
        self.binary_max_length = self.binary_config.get('max_length', 1000)
        self.binary_model = BiLSTMBinary(self.binary_config).to(self.device)
        self.binary_model.load_state_dict(binary_ckpt['model_state_dict'])
        self.binary_model.eval()
        
        # 加载多分类模型
        logger.info(f"Loading multi model: {multi_model_path}")
        multi_ckpt = torch.load(multi_model_path, map_location=self.device, weights_only=False)
        self.multi_config = multi_ckpt['model_config']
        self.class_names = multi_ckpt['class_names']
        self.multi_max_length = multi_ckpt['max_length']
        self.multi_model = BiLSTMClassifier(self.multi_config, len(self.class_names)).to(self.device)
        self.multi_model.load_state_dict(multi_ckpt['model_state_dict'])
        self.multi_model.eval()
        
        logger.info(f"Models loaded. Classes: {self.class_names}")
    
    @torch.no_grad()
    def predict_binary(self, sequences, batch_size=2048, threshold=0.5):
        """
        二分类预测
        
        Returns:
            is_arg: bool数组
            probs: 概率数组
        """
        n = len(sequences)
        is_arg = np.zeros(n, dtype=bool)
        probs = np.zeros(n, dtype=np.float32)
        
        for i in range(0, n, batch_size):
            batch_seqs = sequences[i:i+batch_size]
            # 编码
            encoded = np.array([encode_binary(s, self.binary_max_length) for s in batch_seqs])
            inputs = torch.from_numpy(encoded).to(self.device)
            # 推理
            logits = self.binary_model(inputs)
            batch_probs = torch.sigmoid(logits).cpu().numpy().flatten()
            # 保存结果
            probs[i:i+len(batch_seqs)] = batch_probs
            is_arg[i:i+len(batch_seqs)] = batch_probs >= threshold
        
        return is_arg, probs
    
    @torch.no_grad()
    def predict_multi(self, sequences, batch_size=2048):
        """
        多分类预测
        
        Returns:
            classes: 类别名数组
            probs: 概率数组
        """
        n = len(sequences)
        classes = np.empty(n, dtype=object)
        probs = np.zeros(n, dtype=np.float32)
        
        for i in range(0, n, batch_size):
            batch_seqs = sequences[i:i+batch_size]
            # 编码
            encoded = np.array([encode_multi(s, self.multi_max_length) for s in batch_seqs])
            inputs = torch.from_numpy(encoded).to(self.device)
            # 推理
            logits = self.multi_model(inputs)
            batch_probs = torch.softmax(logits, dim=1)
            max_probs, pred_indices = torch.max(batch_probs, dim=1)
            # 保存结果
            for j, (idx, prob) in enumerate(zip(pred_indices.cpu().numpy(), max_probs.cpu().numpy())):
                classes[i+j] = self.class_names[idx]
                probs[i+j] = prob
        
        return classes, probs
    
    def analyze(self, records, batch_size=2048, threshold=0.5):
        """
        完整分析流程
        
        Args:
            records: [{'file': str, 'id': str, 'seq': str}, ...]
            
        Returns:
            results: DataFrame
        """
        if not records:
            return pd.DataFrame()
        
        sequences = [r['seq'] for r in records]
        
        # Step 1: 二分类
        is_arg, binary_probs = self.predict_binary(sequences, batch_size, threshold)
        
        # Step 2: 对ARG进行多分类
        arg_indices = np.where(is_arg)[0]
        arg_classes = np.empty(len(sequences), dtype=object)
        class_probs = np.zeros(len(sequences), dtype=np.float32)
        
        if len(arg_indices) > 0:
            arg_sequences = [sequences[i] for i in arg_indices]
            classes, probs = self.predict_multi(arg_sequences, batch_size)
            for i, idx in enumerate(arg_indices):
                arg_classes[idx] = classes[i]
                class_probs[idx] = probs[i]
        
        # 构建结果
        results = []
        for i, r in enumerate(records):
            results.append({
                'FileName': r['file'],
                'SequenceID': r['id'],
                'IsARG': is_arg[i],
                'BinaryProb': round(float(binary_probs[i]), 4),
                'ARGClass': arg_classes[i] if is_arg[i] else None,
                'ClassProb': round(float(class_probs[i]), 4) if is_arg[i] else None,
            })
        
        return pd.DataFrame(results)


# ============================================================
# 主流程
# ============================================================

def find_checkpoint(output_path):
    """查找检查点文件"""
    checkpoint_path = output_path + '.checkpoint'
    if os.path.exists(checkpoint_path):
        with open(checkpoint_path, 'r') as f:
            processed_files = set(f.read().strip().split('\n'))
        return processed_files
    return set()


def save_checkpoint(output_path, processed_files):
    """保存检查点"""
    checkpoint_path = output_path + '.checkpoint'
    with open(checkpoint_path, 'w') as f:
        f.write('\n'.join(processed_files))


def main():
    parser = argparse.ArgumentParser(description='ARG识别与分类工作流')
    parser.add_argument('--input_dir', required=True, help='输入文件夹路径 (包含.faa文件)')
    parser.add_argument('--output', default='arg_results.tsv', help='输出TSV文件路径')
    parser.add_argument('--binary_model', default=CONFIG['binary_model'], help='二分类模型路径')
    parser.add_argument('--multi_model', default=CONFIG['multi_model'], help='多分类模型路径')
    parser.add_argument('--threshold', type=float, default=CONFIG['binary_threshold'], help='二分类阈值')
    parser.add_argument('--batch_size', type=int, default=CONFIG['batch_size'], help='批次大小')
    parser.add_argument('--num_workers', type=int, default=CONFIG['num_workers'], help='并行读取文件数')
    parser.add_argument('--file_batch', type=int, default=CONFIG['file_batch_size'], help='每批处理文件数')
    parser.add_argument('--resume', action='store_true', help='从检查点恢复')
    args = parser.parse_args()
    
    # 获取所有文件
    logger.info(f"Scanning directory: {args.input_dir}")
    all_files = glob.glob(os.path.join(args.input_dir, '*.faa'))
    all_files.extend(glob.glob(os.path.join(args.input_dir, '*.fasta')))
    all_files = sorted(all_files)
    logger.info(f"Found {len(all_files)} files")
    
    if not all_files:
        logger.error("No .faa or .fasta files found!")
        sys.exit(1)
    
    # 检查点恢复
    processed_files = set()
    if args.resume:
        processed_files = find_checkpoint(args.output)
        logger.info(f"Resuming from checkpoint: {len(processed_files)} files already processed")
    
    # 过滤已处理文件
    pending_files = [f for f in all_files if os.path.basename(f) not in processed_files]
    logger.info(f"Files to process: {len(pending_files)}")
    
    if not pending_files:
        logger.info("All files already processed!")
        return
    
    # 初始化分析器
    analyzer = ARGAnalyzer(args.binary_model, args.multi_model)
    
    # 统计
    total_sequences = 0
    total_args = 0
    start_time = time.time()
    
    # 输出文件模式
    write_mode = 'a' if args.resume and os.path.exists(args.output) else 'w'
    write_header = write_mode == 'w'
    
    # 批量处理
    for batch_start in tqdm(range(0, len(pending_files), args.file_batch), desc="Processing"):
        batch_files = pending_files[batch_start:batch_start + args.file_batch]
        
        # 并行读取文件
        records = load_files_parallel(batch_files, args.num_workers)
        total_sequences += len(records)
        
        if not records:
            continue
        
        # 分析
        results_df = analyzer.analyze(records, args.batch_size, args.threshold)
        
        # 统计ARG
        arg_count = results_df['IsARG'].sum()
        total_args += arg_count
        
        # 写入结果
        results_df.to_csv(args.output, mode='a', header=write_header, index=False, sep='\t')
        write_header = False
        
        # 更新检查点
        for f in batch_files:
            processed_files.add(os.path.basename(f))
        
        if (batch_start + args.file_batch) % CONFIG['checkpoint_interval'] == 0:
            save_checkpoint(args.output, processed_files)
        
        # 进度日志
        elapsed = time.time() - start_time
        speed = total_sequences / elapsed if elapsed > 0 else 0
        logger.info(f"Progress: {len(processed_files)}/{len(all_files)} files | "
                   f"{total_sequences:,} seqs | {total_args:,} ARGs | "
                   f"{speed:.0f} seqs/s")
    
    # 最终检查点
    save_checkpoint(args.output, processed_files)
    
    # 完成统计
    elapsed = time.time() - start_time
    logger.info("=" * 60)
    logger.info("COMPLETED!")
    logger.info(f"Total files: {len(all_files)}")
    logger.info(f"Total sequences: {total_sequences:,}")
    logger.info(f"Total ARGs: {total_args:,}")
    logger.info(f"ARG ratio: {total_args/total_sequences*100:.2f}%")
    logger.info(f"Time: {elapsed/3600:.2f} hours")
    logger.info(f"Speed: {total_sequences/elapsed:.0f} seqs/s")
    logger.info(f"Output: {args.output}")
    logger.info("=" * 60)


if __name__ == '__main__':
    main()

