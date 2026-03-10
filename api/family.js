import { httpRequest } from "../utils/api"

/**
 * =============================
 * 1️⃣ 家庭组管理接口
 * =============================
 */

// 1.1 创建家庭组
// POST /family/groups
const createGroup = (data) => {
  return httpRequest('/family/groups', 'POST', data)
}

// 1.2 获取自己所在家庭组及成员
// GET /family/groups/me
const getMyGroup = () => {
  return httpRequest('/family/groups/me', 'GET')
}

// 1.3 申请加入家庭组
// POST /family/groups/{groupId}/apply
const applyJoinGroup = (groupId) => {
  return httpRequest(`/family/groups/${groupId}/apply`, 'POST')
}

// 1.4 邀请成员
// POST /family/groups/{groupId}/invite
const inviteMember = (groupId, phoneNumber, data) => {
  return httpRequest(`/family/groups/${groupId}/invite?phoneNumber=${phoneNumber}`, 'POST', data)
}

// 1.5 组长审批申请
// POST /family/groups/{groupId}/approve
const approveApply = (groupId, data) => {
  return httpRequest(`/family/groups/${groupId}/approve`, 'POST', data)
}

// 1.7 成员主动退出
// POST /family/groups/{groupId}/quit
const quitGroup = (groupId) => {
  return httpRequest(`/family/groups/${groupId}/quit`, 'POST')
}

/**
 * =============================
 * 2️⃣ 消息/审批/历史接口
 * =============================
 */

// 2.1 查看本人申请/邀请历史
// GET /family/groups/my/apply-records
const getMyApplyRecords = (params) => {
  return httpRequest('/family/groups/my/apply-records', 'GET', params)
}

// // 2.2 查看本人收到的邀请
// // GET /family/groups/my/invite-records
// const getMyInviteRecords = (params) => {
//   return httpRequest('/family/groups/my/invite-records', 'GET', params)
// }

// 2.3 组长查询待审批申请
// GET /family/groups/{groupId}/pending-applies
const getPendingApplies = (groupId, params) => {
  return httpRequest(`/family/groups/${groupId}/pending-applies`, 'GET', params)
}

/**
 * =============================
 * 3️⃣ 健康数据与异常接口
 * =============================
 */

// 3.1 查询家庭组成员健康与用药数据
// GET /family/groups/{groupId}/health?date=
const getGroupHealthData = (groupId) => {
  return httpRequest(
    `/family/groups/${groupId}/health`,
    'GET'
  )
}

// 3.2 查询当天异常告警
// GET /family/groups/{groupId}/alarms
const getGroupAlarms = (groupId) => {
  return httpRequest(`/family/groups/${groupId}/alarms`, 'GET')
}


/**
 * =============================
 * 导出
 * =============================
 */

export default {
  // 家庭组
  createGroup,
  getMyGroup,
  applyJoinGroup,
  inviteMember,
  approveApply,
  quitGroup,

  // 记录
  getMyApplyRecords,
  // getMyInviteRecords,
  getPendingApplies,

  // 健康
  getGroupHealthData,
  getGroupAlarms
}