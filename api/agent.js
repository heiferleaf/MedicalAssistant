/**
 * AI Agent API 接口
 */

import { httpRequest } from '../utils/api';
import { BASE_URL } from '../config/config';

// OCR
const ocr = (payload) => {
  return httpRequest('/ocr/predict', 'POST', payload)
}

// 健康检查
const health = () => {
  return httpRequest('/agent/health', 'GET')
}

export default {
  // 聊天接口（传统模式）
  chat: (data) => {
    // 真正调用后端 API
    // 后端路径：/api/agent/chat
    // 后端期望参数：user_id, session_id, message (下划线命名)
    return httpRequest('/agent/chat', 'POST', {
      user_id: data.user_id,
      session_id: data.session_id,
      message: data.message
    });
  },
  
  // 流式聊天接口（SSE）
  chatStream: (data) => {
    return new Promise((resolve, reject) => {
      const token = uni.getStorageSync('accessToken');
      const baseUrl = `${BASE_URL}/agent/chat/stream`;
      
      // #ifdef H5
      // === H5 端：使用 EventSource ===
      const params = new URLSearchParams();
      params.append('user_id', data.user_id);
      params.append('session_id', data.session_id);
      params.append('message', data.message);
      
      const eventSource = new EventSource(`${baseUrl}?${params.toString()}&token=${encodeURIComponent(token)}`);
      
      const result = {
        fullMessage: '',
        actionType: null,
        actionData: null
      };
      
      eventSource.addEventListener('message', (event) => {
        if (event.data === '[DONE]') {
          eventSource.close();
          resolve(result);
          return;
        }
        result.fullMessage += event.data;
        if (data.onChunk) {
          data.onChunk(event.data);
        }
      });
      
      eventSource.onerror = (error) => {
        eventSource.close();
        reject(error);
      };
      // #endif
      
      // #ifdef APP-PLUS
      // === App 端：使用 renderjs + fetch-event-source ===
      if (!uni.$sseBridge || !uni.$sseBridge.send) {
        reject(new Error('SSE 桥接未初始化，请确保已在 Assistant.vue 中注册'))
        return
      }
      
      const url = `${baseUrl}?user_id=${data.user_id}&session_id=${data.session_id}&message=${encodeURIComponent(data.message)}&token=${encodeURIComponent(token)}`;
      
      // 使用全局桥接对象调用 SSE 组件
      uni.$sseBridge.send({
        url: url,
        headers: {},
        body: {},
        onMessage: (chunk) => {
          if (data.onChunk) {
            data.onChunk(chunk)
          }
        },
        onDone: () => {
          resolve({
            fullMessage: '',
            actionType: null,
            actionData: null
          })
        },
        onError: (err) => {
          reject(err)
        }
      })
      // #endif
    });
  },
  
  // 健康检查
  health,
  
  // OCR 识别
  ocr,
  
  // 获取待确认的 Tool 请求列表
  getPendingRequests: (userId) => {
    return httpRequest(`/agent/tool-execution/pending?userId=${userId}`, 'GET');
  },
  
  // 批准并执行 Tool
  approveTool: (userId, requestId, editedData) => {
    return httpRequest(`/agent/tool-execution/approve?userId=${userId}&requestId=${requestId}`, 'POST', editedData);
  },
  
  // 拒绝 Tool 请求
  rejectTool: (userId, requestId) => {
    return httpRequest(`/agent/tool-execution/reject?userId=${userId}&requestId=${requestId}`, 'POST');
  },
  
  // 删除用户的所有待确认请求
  deleteAllPending: (userId) => {
    return httpRequest(`/agent/tool-execution/delete-all?userId=${userId}`, 'POST');
  }
};
