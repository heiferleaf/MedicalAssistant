import torch
import torch.nn as nn
from transformers import AutoModel

class CrossAttentionReactionModel(nn.Module):
    """
    交叉注意力架构 (推理版):
    仅包含前向推理逻辑，去除了训练相关的 Loss 计算。
    """
    def __init__(self, model_name, num_labels, reaction_names, dropout_rate=0.1):
        super().__init__()
        
        # BERT编码器
        self.bert = AutoModel.from_pretrained(model_name)
        hidden_size = self.bert.config.hidden_size
        
        self.num_labels = num_labels
        # 推理时不需要 reaction_names 列表本身参与计算，但初始化需要知道长度
        
        # 多头交叉注意力层
        self.cross_attention = nn.MultiheadAttention(
            embed_dim=hidden_size,
            num_heads=8,
            dropout=dropout_rate,
            batch_first=True
        )
        
        # 反应感知的分类头
        self.classifier = nn.Sequential(
            nn.Dropout(dropout_rate),
            nn.Linear(hidden_size * 2, 512),
            nn.ReLU(),
            nn.Dropout(dropout_rate),
            nn.Linear(512, num_labels)
        )
        
        # 注册 buffer 用于存放反应嵌入
        self.register_buffer('reaction_embeddings', torch.zeros(num_labels, hidden_size))

    def forward(self, input_ids, attention_mask):
        """
        推理时的前向传播
        """
        batch_size = input_ids.shape[0]
        
        # Step 1: 编码病例
        outputs = self.bert(input_ids=input_ids, attention_mask=attention_mask)
        sequence_output = outputs.last_hidden_state
        case_cls = sequence_output[:, 0, :]  # (B, hidden_size)
        
        # Step 2: 交叉注意力
        query = case_cls.unsqueeze(1)  # (B, 1, hidden_size)
        
        # 扩展反应嵌入到batch维度
        reaction_emb = self.reaction_embeddings.unsqueeze(0).expand(batch_size, -1, -1)
        
        attended_output, _ = self.cross_attention(
            query=query,
            key=reaction_emb,
            value=reaction_emb
        )
        attended_output = attended_output.squeeze(1)
        
        # Step 3: 拼接
        combined = torch.cat([case_cls, attended_output], dim=1)
        
        # Step 4: 分类
        logits = self.classifier(combined)
        
        return logits