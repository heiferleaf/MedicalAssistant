"""
Predict Service Module
暴露统一的预测接口
"""
from .core import predictor

def predict_reactions(text: str, top_k: int = 5):
    """
    根据输入的病历描述文本，预测可能的不良反应。
    
    Args:
        text (str): 病历描述
        top_k (int): 返回前 K 个可能的结果
        
    Returns:
        list[dict]: [{'reaction': 'Headache', 'probability': 0.95}, ...]
    """
    if not text:
        return []
        
    return predictor.predict(text, top_k=top_k)