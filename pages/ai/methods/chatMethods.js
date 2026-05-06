/**
 * AI 对话方法模块（主文件）
 * 封装 AI 对话核心逻辑（发送消息、图片处理、待确认请求、辅助工具等）
 */

import agentApi from '@/api/agent';
import reminderApi from '@/api/reminder';
import medicineApi from '@/api/medicine';
import { createMessage, createMessageWithAction, StorageKeys, getFromStorage, setToStorage } from '../utils/chatUtils';

export default {
  methods: {
    /**
     * 发送消息主方法
     * @param {string} content - 消息内容
     */
    async sendMessage(content) {
      // 检查是否有图片
      const hasImage = this.showImagePreview && this.scanImage;
      
      // 空值检查
      if (!hasImage && (!content || !content.trim())) {
        console.warn('sendMessage: 消息内容为空，已忽略');
        return;
      }
      
      // 检查是否需要为新会话命名
      const currentSession = this.sessions.find(s => s.sessionId === this.sessionId);
      if (currentSession && currentSession.needsNaming) {
        currentSession.summary = content ? content.substring(0, 20) : '图片识别';
        currentSession.needsNaming = false;
      }
      
      // 添加用户消息
      let userMsg;
      if (hasImage) {
        const fullImageDataUrl = this.scanImageBase64 || this.scanImage;
        // 修复：保留用户实际输入的文字内容
        userMsg = {
          id: Date.now().toString(),
          role: 'user',
          type: 'image',  // 图片消息类型
          content: content,  // 保留用户实际输入的文字
          imagePath: fullImageDataUrl,
          createdAt: new Date().toISOString()
        };
        console.log('📸 创建图片消息:', {
          id: userMsg.id,
          content: userMsg.content,
          imagePathLength: userMsg.imagePath ? userMsg.imagePath.length : 0,
          imagePathPreview: userMsg.imagePath ? userMsg.imagePath.substring(0, 50) + '...' : '无'
        });
      } else {
        userMsg = createMessage('user', content);
      }
      this.messages.push(userMsg);
      this.scrollToBottom();
      
      // 添加加载状态
      const loadingMsg = createMessage('loading', '');
      this.messages.push(loadingMsg);
      this.scrollToBottom();
      this.loading = true;
      
      try {
        // 准备发送给 AI 的消息
        let messageToSend = content || '帮我识别这个药品';
        
        console.log('🔵 原始消息内容:', messageToSend);
        console.log('是否有图片:', hasImage);
        
        // 处理图片消息
        if (hasImage) {
          console.log('📸 开始处理图片消息...');
          messageToSend = await this.handleImageMessage(messageToSend, hasImage);
          console.log('✅ OCR 处理后消息:', messageToSend.substring(0, 200) + '...');
        }
        
        console.log('🚀 最终发送给 AI 的消息:', messageToSend.substring(0, 200) + '...');
        
        // 发送消息并处理流式响应（调用 chatStreamMethods 模块）
        await this.handleStreamResponse(messageToSend, content);
        
        // 保存到本地存储
        this.saveMessagesToStorage();
        
        // 更新会话预览
        if (!currentSession || !currentSession.needsNaming) {
          this.updateSessionPreview(content, this.lastAssistantMessage || '');
        }
      } catch (error) {
        await this.handleSendError(error);
      } finally {
        this.loading = false;
        // 检查是否有待确认的请求
        await this.checkPendingRequests();
      }
    },
    
    /**
     * 处理图片消息
     */
    async handleImageMessage(messageToSend, hasImage) {
      console.log('开始处理图片消息...');
      
      // 检查是否是药物图片路径
      const isDrugImagePath = this.scanImage && this.scanImage.startsWith('/images/drug_');
      
      if (isDrugImagePath) {
        // 药物图片，使用 Base64 数据上传到 Flask OCR
        return await this.processDrugImage(messageToSend);
      } else {
        // 其他图片，使用 OCR 组件识别
        return await this.processGeneralImage(messageToSend);
      }
    },
    
    /**
     * 处理药物图片
     */
    async processDrugImage(messageToSend) {
      try {
        // 1. 获取 Base64 数据
        let base64Data = this.scanImageBase64;
        
        // #ifdef APP-PLUS
        if (!base64Data || base64Data.length < 1000) {
          await this.waitForBase64Conversion();
          base64Data = this.scanImageBase64;
        }
        // #endif
        
        if (!base64Data) {
          base64Data = await this.imageToBase64(this.scanImage);
        }
        
        if (!base64Data) {
          throw new Error('无法获取图片 Base64 数据');
        }
        
        // 2. 转换为临时文件
        const tempFilePath = await this.base64ToFile(base64Data);
        
        // 3. 上传到 Flask OCR
        const ocrResult = await this.uploadToOCR(tempFilePath);
        
        if (ocrResult.success) {
          const ocrOutput = ocrResult.data.output || ocrResult.data.ocr_result || '';
          return `用户发送了一张药物图片，OCR 识别结果：${ocrOutput.substring(0, 200)}。请根据这个结果回答用户问题。`;
        } else {
          throw new Error('OCR 识别失败');
        }
      } catch (e) {
        console.error('OCR 识别异常:', e);
        return `用户发送了一张药物图片，但 OCR 识别失败。请告诉用户重新上传图片。`;
      }
    },
    
    /**
     * 处理普通图片
     */
    async processGeneralImage(messageToSend) {
      try {
        // 1. 获取 Base64 数据
        let base64Data = this.scanImageBase64;
        
        if (!base64Data) {
          base64Data = await this.imageToBase64(this.scanImage);
        }
        
        if (!base64Data) {
          throw new Error('无法获取图片 Base64 数据');
        }
        
        // 2. 使用 OCR 组件识别
        this.ocrLoading = true;
        
        // #ifdef APP-PLUS
        if (this.$refs.ocrUploader && this.$refs.ocrUploader.callOCR) {
          this.$refs.ocrUploader.callOCR({
            base64Data: base64Data,
            ocrUrl: 'http://8.148.94.242:8001/ocr/predict'
          });
        } else {
          throw new Error('OCR 组件未初始化');
        }
        // #endif
        
        // 3. 等待 OCR 结果
        await this.waitForOCRResult();
        
        // 4. 构造消息
        return `【前端已 OCR 识别】药品文字识别结果：${this.ocrResult.substring(0, 200)}。请根据这个 OCR 识别结果回答用户问题。`;
      } catch (e) {
        console.error('OCR 识别异常:', e);
        return `【前端 OCR 识别失败】请告诉用户重新上传图片。`;
      }
    },
    
    /**
     * 等待 Base64 转换完成
     */
    async waitForBase64Conversion() {
      await new Promise((resolve, reject) => {
        let timeout = 0;
        const checkBase64 = setInterval(() => {
          timeout += 100;
          if (timeout > 10000) {
            clearInterval(checkBase64);
            reject(new Error('等待 Base64 转换超时'));
            return;
          }
          if (this.scanImageBase64 && this.scanImageBase64.length > 1000) {
            clearInterval(checkBase64);
            resolve();
          }
        }, 100);
      });
    },
    
    /**
     * 上传到 OCR 服务
     */
    async uploadToOCR(tempFilePath) {
      const ocrUrl = 'http://8.148.94.242:8001/ocr/predict';
      
      return await new Promise((resolve, reject) => {
        // #ifdef H5
        const formData = new FormData();
        formData.append('file', tempFilePath, 'drug.jpg');
        
        fetch(ocrUrl, {
          method: 'POST',
          body: formData
        })
        .then(response => response.json())
        .then(data => {
          resolve({ statusCode: 200, data: JSON.stringify(data) });
        })
        .catch(err => {
          reject(err);
        });
        // #endif
        
        // #ifdef APP-PLUS
        uni.uploadFile({
          url: ocrUrl,
          filePath: tempFilePath,
          name: 'file',
          formData: {},
          success: (res) => {
            resolve(res);
          },
          fail: (err) => {
            reject(err);
          }
        });
        // #endif
      });
    },
    
    /**
     * 等待 OCR 结果
     */
    async waitForOCRResult() {
      await new Promise((resolve, reject) => {
        let timeout = 0;
        const checkResult = setInterval(() => {
          timeout += 100;
          if (timeout > 30000) {
            clearInterval(checkResult);
            reject(new Error('OCR 超时'));
            return;
          }
          if (!this.ocrLoading) {
            clearInterval(checkResult);
            if (this.ocrResult) {
              resolve();
            } else {
              reject(new Error('OCR 失败'));
            }
          }
        }, 100);
      });
    },
    
    /**
     * 处理流式响应（已迁移到 chatStreamMethods.js，此处保留接口）
     */
    async handleStreamResponse(messageToSend, userContent) {
      // 此方法已迁移到 chatStreamMethods.js
      // 为了保持兼容性，这里不做删除，但实际不会调用
      console.warn('handleStreamResponse 已迁移到 chatStreamMethods.js');
    },
    
    /**
     * 处理工具状态（已迁移到 chatStreamMethods.js）
     */
    handleToolStatus(toolStatus, toolSteps, assistantMsg) {
      // 此方法已迁移到 chatStreamMethods.js
      console.warn('handleToolStatus 已迁移到 chatStreamMethods.js');
    },
    
    /**
     * 处理操作卡片确认（已迁移到 chatActionMethods.js）
     */
    async handleActionConfirm({ type, data, messageId }) {
      // 此方法已迁移到 chatActionMethods.js
      console.warn('handleActionConfirm 已迁移到 chatActionMethods.js');
    },
    
    /**
     * 处理操作卡片取消（已迁移到 chatActionMethods.js）
     */
    async handleActionCancel({ type, data, messageId }) {
      // 此方法已迁移到 chatActionMethods.js
      console.warn('handleActionCancel 已迁移到 chatActionMethods.js');
    },
    
    /**
     * 处理操作卡片编辑（已迁移到 chatActionMethods.js）
     */
    handleActionEdit({ type, data }) {
      // 此方法已迁移到 chatActionMethods.js
      console.warn('handleActionEdit 已迁移到 chatActionMethods.js');
    },
    
    /**
     * 创建用药计划（用户确认）（已迁移到 chatActionMethods.js）
     */
    async handleCreatePlanWithApproval(data, messageId) {
      // 此方法已迁移到 chatActionMethods.js
      console.warn('handleCreatePlanWithApproval 已迁移到 chatActionMethods.js');
    },
    
    /**
     * 更新用药任务（已迁移到 chatActionMethods.js）
     */
    async handleUpdateTaskWithApproval(data, messageId) {
      // 此方法已迁移到 chatActionMethods.js
      console.warn('handleUpdateTaskWithApproval 已迁移到 chatActionMethods.js');
    },
    
    /**
     * 添加药品到药箱（已迁移到 chatActionMethods.js）
     */
    async handleAddMedicineWithApproval(data, messageId) {
      // 此方法已迁移到 chatActionMethods.js
      console.warn('handleAddMedicineWithApproval 已迁移到 chatActionMethods.js');
    },
    
    /**
     * 更新卡片状态（已迁移到 chatActionMethods.js）
     */
    updateCardStatus(messageId, status) {
      // 此方法已迁移到 chatActionMethods.js
      console.warn('updateCardStatus 已迁移到 chatActionMethods.js');
    },
    
    /**
     * 检查待确认请求
     */
    async checkPendingRequests() {
      try {
        const response = await agentApi.getPendingRequests(this.userId);
        const responseData = response.data || response;
        
        let dataList = [];
        if (responseData.code === 200 && Array.isArray(responseData.data)) {
          dataList = responseData.data;
        } else if (responseData.success && Array.isArray(responseData.data)) {
          dataList = responseData.data;
        } else if (Array.isArray(responseData)) {
          dataList = responseData;
        }
        
        if (dataList.length > 0) {
          const pending = dataList[0];
          console.log('检测到待确认请求:', pending);
          
          if (pending.toolName === 'createPlan') {
            await this.handlePendingPlan(pending);
          } else if (pending.toolName === 'addMedicine') {
            await this.handlePendingMedicine(pending);
          }
        }
      } catch (error) {
        console.error('检查待确认请求失败:', error);
      }
    },
    
    /**
     * 处理待确认的计划
     */
    async handlePendingPlan(pending) {
      let planData;
      if (typeof pending.toolArguments === 'string') {
        try {
          planData = JSON.parse(pending.toolArguments);
        } catch (e) {
          console.error('解析 plan 参数失败:', e);
          planData = {};
        }
      } else {
        planData = pending.toolArguments;
      }
      
      const msg = createMessageWithAction(
        'assistant',
        '请确认是否创建以下用药计划：',
        'plan',
        {
          data: planData,
          showConfirm: true,
          showEdit: true,
          requestId: pending.requestId
        }
      );
      this.messages.push(msg);
      this.scrollToBottom();
    },
    
    /**
     * 处理待确认的药品
     */
    async handlePendingMedicine(pending) {
      let medicineData;
      if (typeof pending.toolArguments === 'string') {
        try {
          medicineData = JSON.parse(pending.toolArguments);
        } catch (e) {
          console.error('解析 medicine 参数失败:', e);
          medicineData = {};
        }
      } else {
        medicineData = pending.toolArguments;
      }
      
      const msg = createMessageWithAction(
        'assistant',
        '请确认是否添加以下药品到药箱：',
        'medicine',
        {
          data: {
            medicineName: medicineData.medicineName || '',
            defaultDosage: medicineData.defaultDosage || '',
            remark: medicineData.remark || ''
          },
          status: 'pending',
          requestId: pending.requestId
        }
      );
      this.messages.push(msg);
      this.scrollToBottom();
    },
    
    /**
     * 解析 action 数据
     */
    parseActionData(dataStr) {
      const result = {
        medicineName: '',
        timePoint: '08:00',
        dosage: '',
        frequency: '每日一次',
        startDate: new Date().toISOString().split('T')[0],
        endDate: '',
        remark: ''
      };
      
      const lines = dataStr.split('\n');
      for (const line of lines) {
        const [key, value] = line.split(':').map(s => s.trim());
        if (key === 'medicineName') result.medicineName = value;
        else if (key === 'dosage') result.dosage = value;
        else if (key === 'timePoints') {
          const times = value.split(',');
          result.timePoint = times[0] || '08:00';
        }
        else if (key === 'startDate') result.startDate = value;
        else if (key === 'endDate') result.endDate = value || '';
        else if (key === 'remark') result.remark = value || '';
      }
      
      return result;
    },
    
    /**
     * 从消息中提取药品名称
     */
    extractMedicineName(content) {
      const match = content.match(/创建 (?:用药)? 计划 [：:]\s*(.+)/);
      if (match && match[1]) {
        return match[1].trim();
      }
      const match2 = content.match(/创建\s+(.+?)\s+(?:的)? 用药计划/);
      if (match2 && match2[1]) {
        return match2[1].trim();
      }
      const match3 = content.match(/添加 (.+?) 到药箱/);
      if (match3 && match3[1]) {
        return match3[1].trim();
      }
      const match4 = content.match(/(?:吃了 | 服用了 | 吃了)(.+?)(?:药 | 片 | 胶囊|)/);
      if (match4 && match4[1]) {
        return match4[1].trim();
      }
      return '';
    },
    
    /**
     * 从消息中提取剂量
     */
    extractDosage(content) {
      const match = content.match(/(?:剂量 | 每次 | 用量)[：:]\s*(.+?)(?:，|,|。|$)/);
      if (match && match[1]) {
        return match[1].trim();
      }
      const match2 = content.match(/(\d+\s*(?:片 | 粒|mg|ml|克))/);
      if (match2 && match2[1]) {
        return match2[1].trim();
      }
      return '';
    },
    
    /**
     * 从消息中提取时间
     */
    extractTimePoint(content) {
      const match = content.match(/(?:时间 | 在|点)[：:]\s*(\d{1,2}[：:]\d{2})/);
      if (match && match[1]) {
        return match[1].trim();
      }
      const match2 = content.match(/(早上 | 上午 | 下午 | 晚上)\s*(\d{1,2}(?:[：:]\d{2})?)/);
      if (match2 && match2[2]) {
        return match2[2].trim();
      }
      const match3 = content.match(/(\d{1,2}:\d{2})/);
      if (match3 && match3[1]) {
        return match3[1].trim();
      }
      return '';
    },
    
    /**
     * 更新会话预览
     */
    updateSessionPreview(userMessage, assistantMessage) {
      const session = this.sessions.find(s => s.sessionId === this.sessionId);
      if (session) {
        session.lastMessage = assistantMessage;
        if (!session.needsNaming) {
          session.summary = userMessage.substring(0, 20);
        }
        
        const index = this.sessions.indexOf(session);
        if (index > 0) {
          this.sessions.splice(index, 1);
          this.sessions.unshift(session);
        }
      }
    },
    
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
     * 创建用药计划（传统模式）
     */
    async handleCreatePlan(data) {
      uni.showLoading({ title: '创建中...' });
      
      try {
        // 调用后端 API 创建计划
        // 后端需要 timePoints 数组格式
        const timePoints = data.timePoint ? [data.timePoint] : ['08:00'];
        
        const planData = {
          medicineName: data.medicineName,
          dosage: data.dosage || '按医嘱',
          timePoints: timePoints,  // 数组格式
          startDate: data.startDate || new Date().toISOString().split('T')[0]
        };
        
        console.log('创建计划参数:', planData);
        
        const res = await reminderApi.addPlan(parseInt(this.userId), planData);
        
        console.log('API 返回结果:', res);
        
        uni.hideLoading();
        
        // 检查返回结果：如果返回的是对象且有 id 或 medicineName，说明创建成功
        if (res && (res.id || res.medicineName)) {
          console.log('创建成功，返回计划对象:', res);
          
          // 显示成功提示并询问是否跳转
          uni.showModal({
            title: '创建成功',
            content: `已成功创建用药计划：**${res.medicineName || data.medicineName}**\n\n时间：${data.timePoint}\n剂量：${data.dosage}\n\n是否跳转到用药提醒页面查看？`,
            confirmText: '去看看',
            cancelText: '暂不',
            success: (res) => {
              if (res.confirm) {
                // 跳转到用药提醒页面
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
          console.error('创建失败，返回结果:', res);
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
     * 保存消息到本地存储
     */
    saveMessagesToStorage() {
      try {
        const storageKey = `${StorageKeys.MESSAGES}_${this.sessionId}`;
        setToStorage(storageKey, this.messages);
        console.log('消息已保存到本地存储');
      } catch (error) {
        console.error('保存消息失败:', error);
      }
    },
    
    /**
     * 加载更多消息
     */
    loadMoreMessages() {
      console.log('加载更多消息');
      // TODO: 实现分页加载
    },
    
    /**
     * 滚动到指定消息
     */
    scrollToMessage(msgId) {
      this.scrollToMsgId = msgId;
    },
    
    /**
     * 滚动到底部
     */
    scrollToBottom() {
      if (this.messages.length > 0) {
        const lastMsg = this.messages[this.messages.length - 1];
        this.$nextTick(() => {
          this.scrollToMsgId = 'msg-' + lastMsg.id;
        });
      }
    },
    
    /**
     * 更新会话预览
     */
    updateSessionPreview(userMessage, assistantMessage) {
      const session = this.sessions.find(s => s.sessionId === this.sessionId);
      if (session) {
        session.lastMessage = assistantMessage;
        if (!session.needsNaming) {
          session.summary = userMessage.substring(0, 20);
        }
        
        // 移到列表顶部
        const index = this.sessions.indexOf(session);
        if (index > 0) {
          this.sessions.splice(index, 1);
          this.sessions.unshift(session);
        }
      }
    },
    
    /**
     * 解析特殊标记中的数据
     */
    parseActionData(dataStr) {
      const result = {
        medicineName: '',
        timePoint: '08:00',
        dosage: '',
        frequency: '每日一次',
        startDate: new Date().toISOString().split('T')[0],
        endDate: '',
        remark: ''
      };
      
      const lines = dataStr.split('\n');
      for (const line of lines) {
        const [key, value] = line.split(':').map(s => s.trim());
        if (key === 'medicineName') result.medicineName = value;
        else if (key === 'dosage') result.dosage = value;
        else if (key === 'timePoints') {
          // timePoints 是逗号分隔的字符串，取第一个
          const times = value.split(',');
          result.timePoint = times[0] || '08:00';
        }
        else if (key === 'startDate') result.startDate = value;
        else if (key === 'endDate') result.endDate = value || '';
        else if (key === 'remark') result.remark = value || '';
      }
      
      return result;
    },
    
    /**
     * 从用户消息中提取药品名称
     */
    extractMedicineName(content) {
      // 尝试从“创建用药计划 xxx”中提取药品名
      const match = content.match(/创建 (?:用药)? 计划 [：:]\s*(.+)/);
      if (match && match[1]) {
        return match[1].trim();
      }
      // 或者从“我想创建 xxx 的用药计划”中提取
      const match2 = content.match(/创建\s+(.+?)\s+(?:的)? 用药计划/);
      if (match2 && match2[1]) {
        return match2[1].trim();
      }
      // 从“添加 xxx 到药箱”中提取
      const match3 = content.match(/添加 (.+?) 到药箱/);
      if (match3 && match3[1]) {
        return match3[1].trim();
      }
      // 从“吃了 xxx”、“服用了 xxx”中提取
      const match4 = content.match(/(?:吃了 | 服用了 | 吃了)(.+?)(?:药 | 片 | 胶囊 |)/);
      if (match4 && match4[1]) {
        return match4[1].trim();
      }
      return '';
    },
    
    /**
     * 提取剂量
     */
    extractDosage(content) {
      // 尝试从“剂量 xxx”、“每次 xxx”中提取
      const match = content.match(/(?:剂量 | 每次 | 用量)[：:]\s*(.+?)(?:，|,|。|$)/);
      if (match && match[1]) {
        return match[1].trim();
      }
      // 从“xxx 片”、“xxx mg”中提取
      const match2 = content.match(/(\d+\s*(?:片 | 粒|mg|ml|克))/);
      if (match2 && match2[1]) {
        return match2[1].trim();
      }
      return '';
    },
    
    /**
     * 提取时间点
     */
    extractTimePoint(content) {
      // 尝试从“时间 xxx”、“xxx 点”中提取
      const match = content.match(/(?:时间 | 在 | 点)[：:]\s*(\d{1,2}[：:]\d{2})/);
      if (match && match[1]) {
        return match[1].trim();
      }
      // 从“早上 xxx”、“上午 xxx”、“下午 xxx”、“晚上 xxx”中提取
      const match2 = content.match(/(早上 | 上午 | 下午 | 晚上)\s*(\d{1,2}(?:[：:]\d{2})?)/);
      if (match2 && match2[2]) {
        return match2[2].trim();
      }
      // 从“xxx:xx”格式中提取
      const match3 = content.match(/(\d{1,2}:\d{2})/);
      if (match3 && match3[1]) {
        return match3[1].trim();
      }
      return '';
    }
  }
};
