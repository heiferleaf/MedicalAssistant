from flask import Blueprint, request, jsonify
from app.services.predict import predict_reactions
import traceback

# 定义蓝图，设置 url 前缀为 /api/predict
bp = Blueprint('predict', __name__, url_prefix='/api/predict')

@bp.route('/analyze', methods=['POST'])
def analyze_reactions():
    """
    接收 JSON: {"text": "病历文本..."}
    返回 JSON: {"predictions": [...]}
    """
    try:
        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({'error': 'Missing "text" field'}), 400
        
        text = data['text']
        
        # 调用核心预测服务
        # 注意: 第一次调用时会加载模型，速度较慢
        results = predict_reactions(text, top_k=5)
        
        return jsonify({
            'status': 'success',
            'predictions': results
        })
        
    except Exception as e:
        print(f"Prediction Error: {e}")
        traceback.print_exc()
        return jsonify({'error': str(e)}), 500