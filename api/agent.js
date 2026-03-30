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
      // 从 config 导入 BASE_URL
      const baseUrl = `${BASE_URL}/agent/chat/stream`;
      
      // 构建 URL 参数
      const params = new URLSearchParams();
      params.append('user_id', data.user_id);
      params.append('session_id', data.session_id);
      params.append('message', data.message);
      
      // 使用 EventSource 接收 SSE 流
      // 注意：EventSource 不能携带自定义头，所以我们通过 URL 参数传递 token
      const eventSource = new EventSource(`${baseUrl}?${params.toString()}&token=${encodeURIComponent(token)}`);
      
      const result = {
        fullMessage: '',
        actionType: null,
        actionData: null
      };
      
      eventSource.addEventListener('message', (event) => {
        console.log('收到 message 事件:', event.data);
        if (event.data === '[DONE]') {
          return;
        }
        // 逐字返回
        result.fullMessage += event.data;
        if (data.onChunk) {
          data.onChunk(event.data);
        }
      });
      
      // 监听 open 事件
      eventSource.onopen = () => {
        console.log('SSE 连接已建立');
      };
      
      // 监听 error 事件
      eventSource.onerror = (error) => {
        console.error('EventSource 发生错误:', error);
        eventSource.close();
        reject(new Error('SSE 连接错误：' + (error.message || '未知错误')));
      };
      
      eventSource.addEventListener('action', (event) => {
        try {
          const actionData = JSON.parse(event.data);
          result.actionType = actionData.action_type;
          result.actionData = actionData.action_data;
          if (data.onAction) {
            data.onAction(actionData);
          }
        } catch (e) {
          console.error('解析 action 数据失败:', e);
        }
      });
      
      // 监听工具状态事件
      eventSource.addEventListener('tool_status', (event) => {
        try {
          const toolStatus = JSON.parse(event.data);
          if (data.onToolStatus) {
            data.onToolStatus(toolStatus);
          }
        } catch (e) {
          console.error('解析 tool_status 数据失败:', e);
        }
      });
      
      eventSource.addEventListener('error', (event) => {
        console.error('SSE 错误:', event);
        eventSource.close();
        // 有时候云服务器的SSE连接会有CORS问题，但我们仍然尝试获取错误信息
        const errorMessage = event.data || 'SSE 连接错误';
        reject(new Error(errorMessage));
      });
      
      eventSource.addEventListener('end', () => {
        console.log('SSE 连接关闭');
        eventSource.close();
        resolve(result);
      });
      
      // 超时处理
      setTimeout(() => {
        eventSource.close();
        reject(new Error('SSE 请求超时'));
      }, 60000); // 60 秒超时
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
