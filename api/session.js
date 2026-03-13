import { httpRequest } from "../utils/api"

// 获取会话列表
const getSessions = (userId) => {
  return httpRequest(`/agent/sessions?userId=${userId}`, 'GET')
}

// 创建新会话
const createSession = (userId) => {
  return httpRequest('/agent/sessions', 'POST', { userId })
}

// 删除会话
const deleteSession = (sessionId) => {
  return httpRequest(`/agent/sessions/${sessionId}`, 'DELETE')
}

// 获取会话消息
const getMessages = (sessionId, limit = 50) => {
  return httpRequest(`/agent/sessions/${sessionId}/messages?limit=${limit}`, 'GET')
}

// 清空会话消息
const clearMessages = (sessionId) => {
  return httpRequest(`/agent/sessions/${sessionId}/messages`, 'DELETE')
}

export default {
  getSessions,
  createSession,
  deleteSession,
  getMessages,
  clearMessages
}
