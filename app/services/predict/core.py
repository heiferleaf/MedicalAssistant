import os
import torch
import pickle
import numpy as np
from transformers import AutoTokenizer
from .model_def import CrossAttentionReactionModel

# 定义资源路径
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
RESOURCE_DIR = os.path.join(BASE_DIR, 'resources')
MODEL_PATH = os.path.join(RESOURCE_DIR, 'pytorch_model') # 指向权重文件或目录
METADATA_PATH = os.path.join(RESOURCE_DIR, 'metadata.pkl')

class ReactionPredictor:
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ReactionPredictor, cls).__new__(cls)
            cls._instance.initialize()
        return cls._instance
    
    def initialize(self):
        print("正在加载预测模型资源...")
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        
        # 1. 加载 Metadata
        if not os.path.exists(METADATA_PATH):
            raise FileNotFoundError(f"找不到 metadata 文件: {METADATA_PATH}")
            
        with open(METADATA_PATH, 'rb') as f:
            meta = pickle.load(f)
            self.all_reaction_names = meta['all_reaction_names']
            # 假设 config 里有 model_name，如果没有则默认用 biobert
            self.model_name = meta.get('config', {}).get('model_name', 'dmis-lab/biobert-base-cased-v1.1')

        # 2. 初始化 Tokenizer
        # 注意: 生产环境最好把 tokenizer 也下载到 resources 里加载，防止联网失败
        try:
            self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
        except:
            # 如果下载失败，尝试本地路径或者报错提示
            print(f"警告: 无法从 {self.model_name} 下载 tokenizer，尝试加载本地...")
            # self.tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
            raise

        # 3. 初始化并加载模型架构
        self.model = CrossAttentionReactionModel(
            model_name=self.model_name,
            num_labels=len(self.all_reaction_names),
            reaction_names=self.all_reaction_names
        )
        
        # 4. 加载权重
        # 情况A: 如果 pytorch_model 是目录 (HuggingFace Trainer format)
        if os.path.isdir(MODEL_PATH):
             # 这种方式需要你的 Model 类支持 from_pretrained，或者手动加载 .bin
             weights_path = os.path.join(MODEL_PATH, 'pytorch_model.bin')
             state_dict = torch.load(weights_path, map_location=self.device)
        # 情况B: 如果 pytorch_model 是文件
        else:
             state_dict = torch.load(MODEL_PATH, map_location=self.device)
             
        self.model.load_state_dict(state_dict, strict=False) # strict=False 以防 metadata 不匹配
        self.model.to(self.device)
        self.model.eval()
        print("预测模型加载完成 ✅")

    def predict(self, text, top_k=5):
        """
        预测单个文本的反应
        """
        # 预处理
        encoding = self.tokenizer(
            text,
            truncation=True,
            padding=True,
            max_length=512,
            return_tensors='pt'
        )
        
        input_ids = encoding['input_ids'].to(self.device)
        attention_mask = encoding['attention_mask'].to(self.device)
        
        # 推理
        with torch.no_grad():
            logits = self.model(input_ids, attention_mask)
            probs = torch.sigmoid(logits).cpu().numpy()[0] # 转为概率
            
        # 获取 Top-K 结果
        top_indices = np.argsort(probs)[::-1][:top_k]
        
        results = []
        for idx in top_indices:
            results.append({
                'reaction': self.all_reaction_names[idx],
                'probability': float(probs[idx])
            })
            
        return results

# 全局单例
predictor = ReactionPredictor()