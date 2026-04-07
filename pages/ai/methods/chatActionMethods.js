/**
 * AI 对话操作卡片方法模块
 * 封装操作卡片处理逻辑（确认、取消、编辑、创建计划、更新任务、添加药品等）
 */

import reminderApi from '@/api/reminder';
import medicineApi from '@/api/medicine';
import { createMessage } from '../utils/chatUtils';

export default {
  methods: {
    /**
     * 处理操作卡片确认
     */
    async handleActionConfirm({ type, data, messageId }) {
      console.log('操作确认:', type, data, messageId);
      console.log('messageId 类型:', typeof messageId, '值:', messageId);
      
      if (type === 'plan') {
        // 确认创建用药计划
        console.log('准备调用 handleCreatePlanWithApproval, messageId:', messageId);
        await this.handleCreatePlanWithApproval(data, messageId);
      } else if (type === 'task') {
        // 确认更新用药任务状态
        console.log('准备调用 handleUpdateTaskWithApproval, messageId:', messageId);
        await this.handleUpdateTaskWithApproval(data, messageId);
      } else if (type === 'medicine') {
        // 确认添加药品到药箱
        console.log('准备调用 handleAddMedicineWithApproval, messageId:', messageId);
        await this.handleAddMedicineWithApproval(data, messageId);
      }
    },
    
    /**
     * 处理操作卡片取消
     */
    async handleActionCancel({ type, data, messageId }) {
      console.log('操作取消:', type, data, messageId);
      
      if (type === 'plan') {
        // 更新卡片状态为已取消
        this.updateCardStatus(messageId, 'cancelled');
        
        // 显示提示消息
        const cancelMsg = createMessage('assistant', '已取消创建用药计划。如果您需要创建计划，随时告诉我即可。');
        this.messages.push(cancelMsg);
        this.scrollToBottom();
      } else if (type === 'task') {
        // 更新卡片状态为已取消
        this.updateCardStatus(messageId, 'cancelled');
        
        // 显示提示消息
        const cancelMsg = createMessage('assistant', '已取消更新用药任务状态。如果您需要更新，随时告诉我即可。');
        this.messages.push(cancelMsg);
        this.scrollToBottom();
      } else if (type === 'medicine') {
        // 更新卡片状态为已取消
        this.updateCardStatus(messageId, 'cancelled');
        
        // 显示提示消息
        const cancelMsg = createMessage('assistant', '已取消添加药品。如果您需要添加药品，随时告诉我即可。');
        this.messages.push(cancelMsg);
        this.scrollToBottom();
      }
    },
    
    /**
     * 处理操作卡片编辑
     */
    handleActionEdit({ type, data }) {
      console.log('操作编辑:', type, data);
      // 卡片内部会处理编辑逻辑，这里不需要额外操作
    },
    
    /**
     * 创建用药计划（用户确认后调用后端真正创建）
     */
    async handleCreatePlanWithApproval(data, messageId) {
      console.log('handleCreatePlanWithApproval 被调用，messageId:', messageId);
      uni.showLoading({ title: '创建中...' });
      
      try {
        console.log('用户确认创建计划:', data);
        console.log('messageId 在方法内:', messageId);
        
        // 直接调用后端 API 创建计划
        const timePoints = data.timePoint ? [data.timePoint] : ['08:00'];
        
        const planData = {
          medicineName: data.medicineName,
          dosage: data.dosage || '按医嘱',
          timePoints: timePoints,
          startDate: data.startDate || new Date().toISOString().split('T')[0],
          endDate: data.endDate || null,
          remark: data.remark || null
        };
        
        console.log('创建计划参数:', planData);
        
        const res = await reminderApi.addPlan(parseInt(this.userId), planData);
        
        console.log('API 返回结果:', res);
        console.log('res.id:', res?.id, 'res.medicineName:', res?.medicineName);
        
        uni.hideLoading();
        
        // 无论返回什么，先更新卡片状态
        if (messageId) {
          console.log('准备更新卡片状态为 confirmed, messageId:', messageId);
          this.updateCardStatus(messageId, 'confirmed');
        }
        
        if (res && (res.id || res.medicineName)) {
          console.log('创建成功，显示成功提示');
          uni.showModal({
            title: '创建成功',
            content: `已成功创建用药计划：**${res.medicineName || data.medicineName}**\n\n是否跳转到用药提醒页面查看？`,
            confirmText: '去看看',
            cancelText: '暂不',
            success: (modalRes) => {
              if (modalRes.confirm) {
                uni.navigateTo({
                  url: `/pages/reminder/Reminder`
                });
              }
            }
          });
          
          // 在对话中添加确认消息
          const confirmMsg = createMessage('assistant', `✅ 已成功为您创建用药计划：**${res.medicineName || data.medicineName}**\n\n时间：${data.timePoint}\n剂量：${data.dosage}\n频率：${data.frequency || '每日一次'}\n\n您可以在用药提醒页面随时查看和管理此计划。`);
          this.messages.push(confirmMsg);
          this.scrollToBottom();
        } else {
          throw new Error(res?.message || '创建失败');
        }
        
      } catch (error) {
        console.error('创建计划失败:', error);
        uni.hideLoading();
        
        uni.showModal({
          title: '创建失败',
          content: error.message || '抱歉，创建计划失败，请稍后重试',
          showCancel: false
        });
      }
    },
    
    /**
     * 更新用药任务状态（用户确认后调用后端真正更新）
     */
    async handleUpdateTaskWithApproval(data, messageId) {
      console.log('handleUpdateTaskWithApproval 被调用，messageId:', messageId);
      uni.showLoading({ title: '更新中...' });
      
      try {
        console.log('用户确认更新任务状态:', data);
        
        // 如果有 taskId，更新现有任务；否则需要先创建任务
        if (data.taskId) {
          // 调用后端 API 更新任务状态
          const res = await reminderApi.updateTaskStatus(
            data.taskId, 
            parseInt(this.userId), 
            data.status
          );
          
          uni.hideLoading();
          
          // 更新卡片状态
          if (messageId) {
            this.updateCardStatus(messageId, 'confirmed');
          }
          
          // 显示成功提示
          const statusTexts = ['未服用', '已服用', '漏服'];
          const statusText = statusTexts[data.status] || '未知';
          
          const confirmMsg = createMessage('assistant', `✅ 已成功更新用药任务状态：**${data.medicineName}**\n\n时间：${data.timePoint}\n剂量：${data.dosage}\n状态：${statusText}`);
          this.messages.push(confirmMsg);
          this.scrollToBottom();
        } else {
          // 没有 taskId，说明是临时报告服药，不需要实际创建任务
          // 只显示成功提示
          uni.hideLoading();
          
          // 更新卡片状态
          if (messageId) {
            this.updateCardStatus(messageId, 'confirmed');
          }
          
          const statusTexts = ['未服用', '已服用', '漏服'];
          const statusText = statusTexts[data.status] || '未知';
          
          const confirmMsg = createMessage('assistant', `✅ 已记录：**${data.medicineName}**\n\n时间：${data.timePoint}\n剂量：${data.dosage}\n状态：${statusText}\n\n（提示：如需创建长期用药计划，请告诉我"创建用药计划"）`);
          this.messages.push(confirmMsg);
          this.scrollToBottom();
        }
        
      } catch (error) {
        console.error('更新任务状态失败:', error);
        uni.hideLoading();
        
        uni.showModal({
          title: '更新失败',
          content: error.message || '抱歉，更新任务状态失败，请稍后重试',
          showCancel: false
        });
      }
    },
    
    /**
     * 添加药品到药箱（用户确认后调用后端真正添加）
     */
    async handleAddMedicineWithApproval(data, messageId) {
      console.log('handleAddMedicineWithApproval 被调用，messageId:', messageId);
      uni.showLoading({ title: '添加中...' });
      
      try {
        console.log('用户确认添加药品:', data);
        
        // 调用后端 API 添加药品（注意字段名映射：medicineName -> name）
        const medicineData = {
          name: data.medicineName,  // 后端期望的字段名
          defaultDosage: data.defaultDosage || '按医嘱',
          remark: data.remark || ''
        };
        
        console.log('发送给后端的药品数据:', medicineData);
        const res = await medicineApi.addMedicine(medicineData);
        
        uni.hideLoading();
        
        // 更新卡片状态
        if (messageId) {
          this.updateCardStatus(messageId, 'confirmed');
        }
        
        // 显示成功提示
        const confirmMsg = createMessage('assistant', `✅ 已成功添加药品到药箱：**${data.medicineName}**\n\n默认剂量：${data.defaultDosage}\n备注：${data.remark || '无'}`);
        this.messages.push(confirmMsg);
        this.scrollToBottom();
        
      } catch (error) {
        console.error('添加药品失败:', error);
        uni.hideLoading();
        
        uni.showModal({
          title: '添加失败',
          content: error.message || '抱歉，添加药品失败，请稍后重试',
          showCancel: false
        });
      }
    },
    
    /**
     * 更新卡片状态
     */
    updateCardStatus(messageId, status) {
      console.log('开始更新卡片状态:', messageId, status);
      
      // 查找对应的消息
      const index = this.messages.findIndex(m => m.id === messageId);
      console.log('找到消息索引:', index);
      
      if (index !== -1) {
        const message = this.messages[index];
        console.log('消息 actionData:', message.actionData);
        
        if (message.actionData) {
          console.log('更新前的 status:', message.actionData.status);
          
          // 使用 Vue 的响应式更新：替换整个对象
          const updatedMessage = {
            ...message,
            actionData: {
              ...message.actionData,
              status: status
            }
          };
          // 替换消息以触发响应式更新
          this.messages.splice(index, 1, updatedMessage);
          console.log('更新后的 status:', this.messages[index].actionData.status);
          console.log('卡片状态已更新:', messageId, status);
          
          // 保存到本地存储
          this.saveMessagesToStorage();
        } else {
          console.error('actionData 不存在！');
        }
      } else {
        console.error('未找到消息:', messageId);
      }
    }
  }
};
