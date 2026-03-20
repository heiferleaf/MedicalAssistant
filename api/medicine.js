import { httpRequest } from "../utils/api"

// 查询药箱列表
const getMedicineList = () => {
  return httpRequest('/medicine', 'GET')
}

// 新增药品
const addMedicine = (medicineData) => {
  return httpRequest('/medicine', 'POST', medicineData)
}

// 编辑药品
const editMedicine = (medicineId, medicineData) => {
  return httpRequest(`/medicine/${medicineId}`, 'PUT', medicineData)
}

// 删除药品
const deleteMedicine = (medicineId) => {
  return httpRequest(`/medicine/${medicineId}`, 'DELETE')
}

// 从药箱快速创建用药计划
const createMedicinePlan = (medicineId, planData) => {
  return httpRequest(`/medicine/${medicineId}/plan`, 'POST', planData)
}

export default {
  getMedicineList,
  addMedicine,
  editMedicine,
  deleteMedicine,
  createMedicinePlan
}