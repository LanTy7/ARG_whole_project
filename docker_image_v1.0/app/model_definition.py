"""
BiLSTM 模型定义

包含:
- BiLSTMModel: 二分类模型（ARG识别）
- BiLSTMClassifier: 多分类模型（ARG类型分类）

更新说明：
- 与 binary/model_train/train.ipynb 和 multi/model_train/train.ipynb 保持一致
- 多分类模型支持 Masked Global Pooling
"""

import torch
import torch.nn as nn
import torch.nn.functional as F


class BiLSTMModel(nn.Module):
    """
    BiLSTM + Global Pooling 二分类模型
    
    用于判断序列是否为抗性基因(ARG)
    输入: 氨基酸序列的索引编码 (batch, seq_len)
    输出: logits (batch, 1)
    """
    
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
        
        # 双向LSTM输出 * 2 (max + avg pooling) = hidden_size * 4
        self.classifier = nn.Sequential(
            nn.Linear(config['hidden_size'] * 4, config['hidden_size']),
            nn.ReLU(),
            nn.Dropout(config['dropout']),
            nn.Linear(config['hidden_size'], 1)
        )

    def forward(self, x):
        emb = self.embedding(x)          # (batch, seq_len, embed_dim)
        output, _ = self.lstm(emb)       # (batch, seq_len, hidden*2)
        
        # Global Pooling
        max_pool, _ = torch.max(output, dim=1)
        avg_pool = torch.mean(output, dim=1)
        features = torch.cat([max_pool, avg_pool], dim=1)  # (batch, hidden*4)
        
        return self.classifier(self.dropout(features))


class FocalLoss(nn.Module):
    """Focal Loss with Label Smoothing for imbalanced classification"""
    
    def __init__(self, alpha=None, gamma=2.0, label_smoothing=0.0):
        super().__init__()
        self.alpha = alpha  # 类别权重
        self.gamma = gamma
        self.label_smoothing = label_smoothing
    
    def forward(self, inputs, targets):
        ce_loss = F.cross_entropy(
            inputs, targets, weight=self.alpha, 
            reduction='none', label_smoothing=self.label_smoothing
        )
        pt = torch.exp(-ce_loss)
        focal_loss = ((1 - pt) ** self.gamma) * ce_loss
        return focal_loss.mean()


class BiLSTMClassifier(nn.Module):
    """
    BiLSTM + Masked Global Pooling 多分类模型
    
    用于对ARG序列进行类型分类
    输入: One-hot编码的氨基酸序列 (batch, seq_len, 21)
    输出: logits (batch, num_classes)
    
    更新：使用 Masked Global Pooling 避免 PAD 位置污染
    """
    
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
        
        # 双向LSTM输出 * 2 (max + avg pooling) = hidden_size * 4
        self.classifier = nn.Sequential(
            nn.Linear(config['hidden_size'] * 4, config['hidden_size']),
            nn.ReLU(),
            nn.Dropout(config['dropout']),
            nn.Linear(config['hidden_size'], num_classes)
        )

    def forward(self, x):
        """
        Args:
            x: One-hot encoded sequences, shape (batch, seq_len, 21)
               其中最后一维 21 表示 20 个氨基酸 + PAD
        """
        output, _ = self.lstm(x)  # (batch, seq_len, hidden*2)
        
        # Masked Global Pooling（避免 PAD 位置污染 max/mean pooling）
        # x 的最后一维为 one-hot(21)，其中 index=20 表示 PAD
        pad_flag = x[:, :, 20]  # (batch, seq_len)
        valid_mask = pad_flag < 0.5  # True 表示真实氨基酸位置
        mask = valid_mask.unsqueeze(-1)  # (batch, seq_len, 1)
        
        # max pooling：把 PAD 位置设为极小值
        output_masked = output.masked_fill(~mask, -1e9)
        max_pool, _ = torch.max(output_masked, dim=1)
        
        # mean pooling：只对真实位置求平均
        mask_f = mask.float()
        sum_pool = (output * mask_f).sum(dim=1)
        denom = mask_f.sum(dim=1).clamp(min=1.0)
        avg_pool = sum_pool / denom
        features = torch.cat([max_pool, avg_pool], dim=1)
        
        return self.classifier(self.dropout(features))


# 向后兼容别名
BiLSTMBinary = BiLSTMModel
