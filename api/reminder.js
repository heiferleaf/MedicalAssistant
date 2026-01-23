import { httpRequest } from "../utils/api"

/**
 * 用药计划相关接口
 * 文档来源: 
 */

// 1. 获取用药计划列表
// 接口: GET /plan?UserId= 
const getPlanList = (userId) => {
  // GET请求通常将参数放在 query 中，这里传入对象 { userId: ... }
  return httpRequest('/plan', 'GET', { userId })
}

// 2. 新增用药计划
// 接口: POST /plan?userId= 
// 注意：文档要求 userId 在 URL query 中，而具体计划数据在 Body 中
const addPlan = (userId, planData) => {
  return httpRequest(`/plan?userId=${userId}`, 'POST', planData)
}

// 3. 编辑用药计划
// 接口: PUT /plan/{planId}?userId= 
const editPlan = (planId, userId, planData) => {
  return httpRequest(`/plan/${planId}?userId=${userId}`, 'PUT', planData)
}

// 4. 删除用药计划
// 接口: DELETE /plan/{planId}?userId= 
const deletePlan = (planId, userId) => {
  return httpRequest(`/plan/${planId}?userId=${userId}`, 'DELETE')
}

/**
 * 用药任务相关接口
 * 文档来源: 
 */

// 5. 获取今日任务列表
// 接口: GET /task/today?userid= 
const getTodayTasks = (userId) => {
  return httpRequest('/task/today', 'GET', { userId })
}

// 6. 修改任务状态
// 接口: PUT /task/{taskId}/status?userId= 
// Body: { "status": 1 } 
const updateTaskStatus = (taskId, userId, status) => {
  return httpRequest(`/task/${taskId}/status?userId=${userId}`, 'PUT', { status })
}

// 7. 查询历史任务
// 接口: GET /task/history?userId=&start=... 
// params包含: userId, start, end, medicineName, status
const getTaskHistory = (params) => {
  return httpRequest('/task/history', 'GET', params)
}

export default {
  getPlanList,
  addPlan,
  editPlan,
  deletePlan,
  getTodayTasks,
  updateTaskStatus,
  getTaskHistory
}