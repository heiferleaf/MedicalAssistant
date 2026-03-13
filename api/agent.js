import { httpRequest } from "../utils/api"

// AI 对话 API
const chat = (payload) => {
  return httpRequest('/agent/chat', 'POST', payload)
}

// 健康检查 API
const health = () => {
  return httpRequest('/agent/health', 'GET')
}

export default {
  chat,
  health
}
