"""
ARG 预测器

提供两阶段预测:
1. 二分类: 判断序列是否为ARG
2. 多分类: 对ARG进行类型分类
"""

import os
import torch
import numpy as np
from io import StringIO
from Bio import SeqIO
from model_definition import BiLSTMBinary, BiLSTMClassifier


class ARGPredictor:
    """ARG序列预测器"""
    
    def __init__(self, model_dir="/app/models", device=None):
        """
        初始化预测器
        
        Args:
            model_dir: 模型文件目录，包含 binary_model.pth 和 multi_model.pth
            device: 计算设备，默认自动选择
        """
        self.device = device if device else torch.device(
            "cuda" if torch.cuda.is_available() else "cpu"
        )
        
        binary_path = os.path.join(model_dir, "binary_model.pth")
        multi_path = os.path.join(model_dir, "multi_model.pth")
        
        # ================== 加载二分类模型 ==================
        if not os.path.exists(binary_path):
            raise FileNotFoundError(f"未找到二分类模型: {binary_path}")
        
        binary_ckpt = torch.load(binary_path, map_location=self.device, weights_only=False)
        self.binary_config = binary_ckpt['config']
        self.binary_max_length = self.binary_config.get('max_length', 1000)
        
        self.binary_model = BiLSTMBinary(self.binary_config).to(self.device)
        self.binary_model.load_state_dict(binary_ckpt['model_state_dict'])
        self.binary_model.eval()
        
        # ================== 加载多分类模型 ==================
        if not os.path.exists(multi_path):
            raise FileNotFoundError(f"未找到多分类模型: {multi_path}")
        
        multi_ckpt = torch.load(multi_path, map_location=self.device, weights_only=False)
        self.multi_config = multi_ckpt['model_config']
        self.class_names = multi_ckpt['class_names']
        self.multi_max_length = multi_ckpt['max_length']
        
        self.multi_model = BiLSTMClassifier(
            self.multi_config, len(self.class_names)
        ).to(self.device)
        self.multi_model.load_state_dict(multi_ckpt['model_state_dict'])
        self.multi_model.eval()
        
        print(f"[INFO] 模型加载完成 | Device: {self.device}")
        print(f"[INFO] 二分类配置: {self.binary_config}")
        print(f"[INFO] 多分类类别: {self.class_names}")

    def _preprocess_binary(self, sequence):
        """
        二分类模型预处理: 序列转索引
        """
        # 氨基酸索引映射 (与训练一致)
        aa_dict = {aa: i + 1 for i, aa in enumerate('ACDEFGHIKLMNPQRSTVWY')}
        aa_dict.update({'X': 21, 'PAD': 0})
        
        max_len = self.binary_max_length
        indices = [aa_dict.get(aa, 21) for aa in sequence.upper()]
        indices = indices[:max_len]
        
        if len(indices) < max_len:
            indices += [0] * (max_len - len(indices))
        
        return torch.tensor([indices], dtype=torch.long).to(self.device)

    def _preprocess_multi(self, sequence):
        """
        多分类模型预处理: One-hot编码
        """
        # 氨基酸编码字典 (与训练一致)
        amino_acids = 'ACDEFGHIKLMNPQRSTVWY'
        aa_dict = {aa: i for i, aa in enumerate(amino_acids)}
        aa_dict.update({
            'B': [aa_dict['D'], aa_dict['N']],  # Asp or Asn
            'Z': [aa_dict['E'], aa_dict['Q']],  # Glu or Gln
            'J': [aa_dict['I'], aa_dict['L']],  # Ile or Leu
            'X': 'ANY',
            'PAD': 20
        })
        
        max_len = self.multi_max_length
        sequence = sequence.upper()
        encoding = np.zeros((max_len, 21), dtype=np.float32)
        
        for i in range(min(len(sequence), max_len)):
            aa = sequence[i]
            if aa in aa_dict:
                idx = aa_dict[aa]
                if isinstance(idx, list):  # 模糊氨基酸
                    for j in idx:
                        encoding[i, j] = 0.5
                elif idx == 'ANY':  # 未知氨基酸
                    encoding[i, :20] = 0.05
                else:
                    encoding[i, idx] = 1.0
            else:
                encoding[i, :20] = 0.05  # 未知字符
        
        # 填充位置
        if len(sequence) < max_len:
            encoding[len(sequence):, 20] = 1.0
        
        return torch.tensor([encoding], dtype=torch.float32).to(self.device)

    def predict_sequence(self, sequence_id, sequence_str, threshold=0.5, top_k=5):
        """
        预测单条序列
        
        Args:
            sequence_id: 序列ID
            sequence_str: 氨基酸序列字符串
            threshold: 二分类阈值 (默认0.5)
            top_k: 返回概率最高的前k个分类 (默认5)
        
        Returns:
            dict: 预测结果
        """
        # 清理序列
        sequence_str = sequence_str.upper().replace("*", "").replace("-", "")
        
        # Step 1: 二分类 - 判断是否为ARG
        input_binary = self._preprocess_binary(sequence_str)
        with torch.no_grad():
            logit = self.binary_model(input_binary)
            prob = torch.sigmoid(logit).item()
        
        if prob < threshold:
            return {
                "id": sequence_id,
                "is_arg": False,
                "binary_prob": round(prob, 4),
                "arg_class": None,
                "class_prob": None,
                "top_classes": None
            }
        
        # Step 2: 多分类 - 确定ARG类型，返回 Top-K
        input_multi = self._preprocess_multi(sequence_str)
        with torch.no_grad():
            logits = self.multi_model(input_multi)
            probs = torch.softmax(logits, dim=1).squeeze(0)  # shape: (num_classes,)
            
            # 获取 Top-K 概率和索引
            k = min(top_k, len(self.class_names))
            top_probs, top_indices = torch.topk(probs, k)
            
            # 构建 Top-K 列表
            top_classes = []
            for i in range(k):
                top_classes.append({
                    "class": self.class_names[top_indices[i].item()],
                    "prob": round(top_probs[i].item(), 4)
                })
        
        return {
            "id": sequence_id,
            "is_arg": True,
            "binary_prob": round(prob, 4),
            "arg_class": self.class_names[top_indices[0].item()],
            "class_prob": round(top_probs[0].item(), 4),
            "top_classes": top_classes
        }

    def predict_batch(self, sequences, seq_ids, threshold=0.5, top_k=5):
        """
        批量预测序列
        
        Args:
            sequences: 序列列表
            seq_ids: 序列ID列表
            threshold: 二分类阈值
            top_k: 返回概率最高的前k个分类 (默认5)
        
        Returns:
            list[dict]: 预测结果列表
        """
        results = []
        
        # 清理序列
        clean_seqs = [s.upper().replace("*", "").replace("-", "") for s in sequences]
        
        # Step 1: 批量二分类
        binary_inputs = []
        for seq in clean_seqs:
            binary_inputs.append(self._preprocess_binary(seq).squeeze(0))
        binary_batch = torch.stack(binary_inputs)
        
        with torch.no_grad():
            binary_logits = self.binary_model(binary_batch)
            binary_probs = torch.sigmoid(binary_logits).cpu().numpy().flatten()
        
        # 找出预测为ARG的序列
        arg_indices = [i for i, p in enumerate(binary_probs) if p >= threshold]
        
        # Step 2: 对ARG序列进行多分类，获取 Top-K
        top_probs_batch = None
        top_indices_batch = None
        if arg_indices:
            multi_inputs = []
            for i in arg_indices:
                multi_inputs.append(self._preprocess_multi(clean_seqs[i]).squeeze(0))
            multi_batch = torch.stack(multi_inputs)
            
            with torch.no_grad():
                multi_logits = self.multi_model(multi_batch)
                multi_probs = torch.softmax(multi_logits, dim=1)
                
                # 获取 Top-K
                k = min(top_k, len(self.class_names))
                top_probs_batch, top_indices_batch = torch.topk(multi_probs, k, dim=1)
                top_probs_batch = top_probs_batch.cpu().numpy()
                top_indices_batch = top_indices_batch.cpu().numpy()
        
        # 组装结果
        arg_result_idx = 0
        k = min(top_k, len(self.class_names))
        for i in range(len(sequences)):
            if binary_probs[i] < threshold:
                results.append({
                    "id": seq_ids[i],
                    "is_arg": False,
                    "binary_prob": round(float(binary_probs[i]), 4),
                    "arg_class": None,
                    "class_prob": None,
                    "top_classes": None
                })
            else:
                # 构建 Top-K 列表
                top_classes = []
                for j in range(k):
                    top_classes.append({
                        "class": self.class_names[top_indices_batch[arg_result_idx][j]],
                        "prob": round(float(top_probs_batch[arg_result_idx][j]), 4)
                    })
                
                results.append({
                    "id": seq_ids[i],
                    "is_arg": True,
                    "binary_prob": round(float(binary_probs[i]), 4),
                    "arg_class": self.class_names[top_indices_batch[arg_result_idx][0]],
                    "class_prob": round(float(top_probs_batch[arg_result_idx][0]), 4),
                    "top_classes": top_classes
                })
                arg_result_idx += 1
        
        return results

    def process_fasta(self, fasta_content, threshold=0.5, top_k=5):
        """
        处理FASTA格式内容
        
        Args:
            fasta_content: FASTA格式字符串
            threshold: 二分类阈值
            top_k: 返回概率最高的前k个分类 (默认5)
        
        Returns:
            list[dict]: 预测结果列表
        """
        results = []
        fasta_io = StringIO(fasta_content)
        
        for record in SeqIO.parse(fasta_io, "fasta"):
            res = self.predict_sequence(
                record.id, 
                str(record.seq), 
                threshold,
                top_k
            )
            results.append(res)
        
        return results

    def process_fasta_file(self, file_path, threshold=0.5, batch_size=256, top_k=5):
        """
        处理FASTA文件 (支持批量处理)
        
        Args:
            file_path: FASTA文件路径
            threshold: 二分类阈值
            batch_size: 批次大小
            top_k: 返回概率最高的前k个分类 (默认5)
        
        Returns:
            list[dict]: 预测结果列表
        """
        all_results = []
        records = list(SeqIO.parse(file_path, "fasta"))
        
        # 批量处理
        for i in range(0, len(records), batch_size):
            batch_records = records[i:i+batch_size]
            sequences = [str(r.seq) for r in batch_records]
            seq_ids = [r.id for r in batch_records]
            
            batch_results = self.predict_batch(sequences, seq_ids, threshold, top_k)
            all_results.extend(batch_results)
        
        return all_results

