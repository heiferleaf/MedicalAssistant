import torch
from app.services.federated.privacy import PrivacyEngine

class FederatedClient:
    def __init__(self):
        self.client_id = "hospital_node_01"
    
    def run_local_round(self):
        """
        执行一轮联邦学习本地任务
        """
        print(f"[{self.client_id}] 1. 开始加载本地模型...")
        
        # --- 模拟：这里通常会加载 Bio_ClinicalBERT 模型 ---
        # 为了演示，我们手动创建一个模拟的模型参数字典
        # 假设这是经过本地数据（如 agent.sqlite3 中的数据）微调后的参数
        simulated_weights = {
            "encoder.layer.0.attention.self.query.weight": torch.randn(768, 768),
            "classifier.weight": torch.randn(2, 768)
        }
        
        # 计算一下原始参数的特征（用于对比）
        original_stats = torch.mean(simulated_weights["classifier.weight"]).item()
        
        # --- 核心：应用隐私保护 ---
        print(f"[{self.client_id}] 2. 正在应用差分隐私保护 (加噪 & 裁剪)...")
        privacy_engine = PrivacyEngine()
        
        # noise_multiplier=0.1 表示添加中等程度的噪声
        protected_weights = privacy_engine.apply_differential_privacy(
            simulated_weights, 
            max_grad_norm=1.0, 
            noise_multiplier=0.1
        )
        
        # 计算加噪后的特征
        protected_stats = torch.mean(protected_weights["classifier.weight"]).item()
        
        # 准备返回结果
        result = {
            "client_id": self.client_id,
            "status": "ready_to_upload",
            "privacy_mechanism": "Gaussian Noise (Differential Privacy)",
            "data_comparison": {
                "original_mean": original_stats,
                "protected_mean": protected_stats,
                "diff": abs(original_stats - protected_stats),
                "privacy_safe": original_stats != protected_stats 
            }
        }
        
        return result