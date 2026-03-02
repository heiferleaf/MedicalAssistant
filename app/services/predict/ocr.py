import os
import io
import numpy as np
import cv2
from PIL import Image
from paddleocr import PaddleOCR
from paddlenlp import Taskflow

# --- 全局单例模型初始化 ---
_ocr_instance = None
_ie_instance = None

def get_ocr_instance():
    global _ocr_instance
    if _ocr_instance is None:
        # 仅在第一次调用时加载 PaddleOCR
        _ocr_instance = PaddleOCR(
            use_angle_cls=False, 
            lang="ch", 
            det_db_box_thresh=0.3, 
            use_dilation=True
        )
    return _ocr_instance

def get_ie_instance():
    global _ie_instance
    if _ie_instance is None:
        # 仅在第一次调用时加载 UIE (Taskflow)
        # 获取当前文件所在目录，定位到 checkpoint 绝对路径
        current_dir = os.path.dirname(os.path.abspath(__file__))
        checkpoint_path = os.path.abspath(os.path.join(current_dir, "../../../checkpoint"))
        
        schema = ["药品名称", "规格", "用法用量", "适应症", "生产日期", "有效期", "成分", "成份", "贮藏", "注意事项", "不良反应"]
        
        _ie_instance = Taskflow(
            'information_extraction', 
            schema=schema, 
            task_path=checkpoint_path
        )
    return _ie_instance

# --- 核心处理逻辑 ---

def main(img_data):
    """
    接收图像字节流，经过 OCR + UIE 提取结果
    """
    # 1. 初始化模型（如果已加载则直接返回实例）
    ocr = get_ocr_instance()
    ie = get_ie_instance()

    try:
        # 2. 图像处理：字节流 -> PIL -> Numpy (OpenCV格式)
        image = Image.open(io.BytesIO(img_data)).convert('RGB')
        img_np = np.array(image)
        img_np = cv2.cvtColor(img_np, cv2.COLOR_RGB2BGR)

        # 3. 执行 OCR 识别
        ocr_result = ocr.ocr(img_np, cls=False)
        
        # 4. 提取并合并 OCR 文本
        all_txt = ""
        if ocr_result and ocr_result[0]:
            # line[1][0] 是识别出的文字内容
            all_txt = "".join([line[1][0] for line in ocr_result[0] if line])
        
        if not all_txt.strip():
            return "未检测到有效文字内容"

        # 5. 执行 UIE 信息提取
        ie_result = ie(all_txt)

        # 6. 解析提取结果为格式化文本
        final_output_text = ""
        if ie_result and isinstance(ie_result[0], dict):
            # 获取识别出来的字典项
            data_dict = ie_result[0]
            for key, values in data_dict.items():
                # 将一个字段下的多个结果用逗号合并
                text_content = ", ".join([v['text'] for v in values if 'text' in v])
                final_output_text += f"{key}: {text_content}\n"
        
        return all_txt, final_output_text

    except Exception as e:
        return f"处理过程中发生错误: {str(e)}"