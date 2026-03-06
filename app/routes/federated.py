from flask import Blueprint, jsonify
from app.services.federated.client import FederatedClient

# 创建蓝图
bp = Blueprint('federated', __name__, url_prefix='/api/federated')

@bp.route('/start_round', methods=['POST'])
def start_federated_round():
    """
    触发接口：开始一次联邦学习本地训练并加噪
    """
    try:
        # 初始化客户端
        client = FederatedClient()
        
        # 运行本地处理流程
        result = client.run_local_round()
        
        return jsonify({
            "message": "Local federated learning round completed successfully.",
            "data": result
        }), 200
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@bp.route('/status', methods=['GET'])
def get_status():
    """
    检查节点状态
    """
    return jsonify({
        "status": "online", 
        "role": "client_node",
        "supported_privacy": "Differential Privacy"
    }), 200