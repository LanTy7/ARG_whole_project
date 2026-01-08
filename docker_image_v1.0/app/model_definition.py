"""
BiLSTM 模型定义

包含:
- BiLSTMBinary: 二分类模型（ARG识别）
- BiLSTMClassifier: 多分类模型（ARG类型分类）
"""

import torch
import torch.nn as nn


class BiLSTMBinary(nn.Module):
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


class BiLSTMClassifier(nn.Module):
    """
    BiLSTM + Global Pooling 多分类模型
    
    用于对ARG序列进行类型分类
    输入: One-hot编码的氨基酸序列 (batch, seq_len, 21)
    输出: logits (batch, num_classes)
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
        output, _ = self.lstm(x)  # (batch, seq_len, hidden*2)
        
        # Global Pooling
        max_pool, _ = torch.max(output, dim=1)
        avg_pool = torch.mean(output, dim=1)
        features = torch.cat([max_pool, avg_pool], dim=1)
        
        return self.classifier(self.dropout(features))

