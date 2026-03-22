import { httpRequest } from "../utils/api"

// AI 对话 API
const chat = (payload) => {
  return httpRequest('/agent/chat', 'POST', payload)
}

// 健康检查 API
const health = () => {
  return httpRequest('/agent/health', 'GET')
}

// OCR
const ocr = (payload) => {
  return httpRequest('/ocr/predict', 'POST', payload)
}

export default {
  chat,
  health,
  ocr
}
