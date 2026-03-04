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
    _is_initialized = False  # 新增状态标志
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ReactionPredictor, cls).__new__(cls)
            # 修改点 1: 移除这里的 .initialize() 调用
            # cls._instance.initialize() 
        return cls._instance
    
    def initialize(self):
        """
        实际加载模型的方法，由 predict 方法按需调用
        """
        print("正在加载预测模型资源 (Lazy Load)...")
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        
        # 1. 加载 Metadata
        if not os.path.exists(METADATA_PATH):
            raise FileNotFoundError(f"找不到 metadata 文件: {METADATA_PATH}")
            
        with open(METADATA_PATH, 'rb') as f:
            meta = pickle.load(f)
            if 'selected_reactions' in meta:
                self.all_reaction_names = meta['selected_reactions']
            elif 'all_reaction_names' in meta:
                self.all_reaction_names = meta['all_reaction_names']
            else:
                if 'mlb' in meta:
                    self.all_reaction_names = list(meta['mlb'].classes_)
                else:
                    raise KeyError(f"metadata中找不到类别名称，现有键: {list(meta.keys())}")

            # 智能路径适配逻辑
            raw_model_name = meta.get('config', {}).get('model_name', 'dmis-lab/biobert-base-cased-v1.1')
            if os.path.exists(raw_model_name):
                self.model_name = raw_model_name
            else:
                folder_name = os.path.basename(raw_model_name.rstrip('/\\'))
                local_candidate = os.path.join(RESOURCE_DIR, folder_name)
                if os.path.exists(local_candidate):
                    print(f"自动定位到本地资源目录: {local_candidate}")
                    self.model_name = local_candidate
                else:
                    print(f"警告: 资源未找到，将尝试在线下载: {folder_name}")
                    self.model_name = 'dmis-lab/biobert-base-cased-v1.1'

        # 2. 初始化 Tokenizer
        try:
            self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
        except Exception as e:
            print(f"Tokenizer加载失败: {e}")
            raise

        # 3. 初始化并加载模型架构
        self.model = CrossAttentionReactionModel(
            model_name=self.model_name,
            num_labels=len(self.all_reaction_names),
            reaction_names=self.all_reaction_names
        )
        
        # 4. 加载权重
        if os.path.isdir(MODEL_PATH):
             weights_path = os.path.join(MODEL_PATH, 'pytorch_model.bin')
             if not os.path.exists(weights_path):
                 # 兼容 safetensors
                 weights_path = os.path.join(MODEL_PATH, 'model.safetensors')
                 
             if not os.path.exists(weights_path):
                 raise FileNotFoundError(f"在目录 {MODEL_PATH} 中找不到权重文件")
                 
             # 根据扩展名选择加载方式，这里简化处理，假设 torch.load 能处理
             state_dict = torch.load(weights_path, map_location=self.device)
        else:
             if not os.path.exists(MODEL_PATH):
                 raise FileNotFoundError(f"找不到权重文件: {MODEL_PATH}")
             state_dict = torch.load(MODEL_PATH, map_location=self.device)
             
        self.model.load_state_dict(state_dict, strict=False)
        self.model.to(self.device)
        self.model.eval()
        
        # 修改点 2: 设置标志位
        self._is_initialized = True
        print("预测模型加载完成 ✅")

    def predict(self, text, top_k=5):
        """
        预测单个文本的反应
        """
        # 修改点 3: 检查是否初始化，如果没有则初始化
        if not self._is_initialized:
            self.initialize()

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
            probs = torch.sigmoid(logits).cpu().numpy()[0]
            
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
# 这行代码现在非常轻量，只会创建一个空对象，不会触发加载
predictor = ReactionPredictor()