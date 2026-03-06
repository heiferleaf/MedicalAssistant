import torch
import numpy as np

class PrivacyEngine:
    """
    负责处理联邦学习中的差分隐私保护逻辑
    """
    
    @staticmethod
    def apply_differential_privacy(model_state_dict, max_grad_norm=1.0, noise_multiplier=0.1):
        """
        对模型参数应用差分隐私：
        1. 裁剪 (Clipping): 限制参数更新的幅度
        2. 加噪 (Noising): 添加高斯噪声
        """
        protected_update = {}
        
        # 遍历每一层参数
        for layer_name, param_tensor in model_state_dict.items():
            # 只有浮点数参数才需要加噪（整数类型的配置项不需要）
            if isinstance(param_tensor, torch.Tensor) and param_tensor.is_floating_point():
                # 1. 计算范数 (L2 Norm)
                l2_norm = torch.norm(param_tensor, p=2)
                
                # 2. 裁剪 (Clipping): 如果范数超过阈值，则按比例缩小
                clip_factor = max_grad_norm / (l2_norm + 1e-6)
                clip_factor = torch.min(clip_factor, torch.tensor(1.0))
                clipped_param = param_tensor * clip_factor
                
                # 3. 加噪 (Noising): 添加高斯噪声
                # 噪声标准差 = 裁剪阈值 * 噪声乘数
                noise_std = max_grad_norm * noise_multiplier
                noise = torch.normal(mean=0.0, std=noise_std, size=param_tensor.size())
                
                # 生成保护后的参数
                protected_update[layer_name] = clipped_param + noise
            else:
                # 其他参数原样保留
                protected_update[layer_name] = param_tensor
                
        return protected_update