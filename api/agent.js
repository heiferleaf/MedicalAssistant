/**
 * AI Agent API 接口
 */

import { httpRequest } from '../utils/api';

export default {
  // 聊天接口
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
  }
};