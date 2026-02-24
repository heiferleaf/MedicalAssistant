from typing import Any
from flask import Blueprint, jsonify, request
from app.services.predict.ocr import main

ocr_bp = Blueprint("ocr", __name__)

@ocr_bp.get("/health")
def ocr_health() -> Any:
    return jsonify({"status": "ok", "module": "ocr"})

@ocr_bp.post("/predict")  # 改为 POST 请求用于上传
def ocr_predict() -> Any:
    # 1. 检查请求中是否有文件
    if 'file' not in request.files:
        return jsonify({"status": "error", "message": "没有上传图片"}), 400
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({"status": "error", "message": "文件名为空"}), 400

    try:
        # 2. 读取文件的二进制内容
        img_bytes = file.read()
        
        # 3. 传入 main 函数进行预测
        ocr_result, output = main(img_bytes)
        
        # 4. 返回识别结果
        return jsonify({
            "status": "success",
            "ocr_result": ocr_result,
            "output": output
        })
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500