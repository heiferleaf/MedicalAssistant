/**
 * AI 对话流式响应方法模块
 * 封装流式响应处理逻辑（接收流式数据、处理 action、处理工具状态等）
 */

import agentApi from '@/api/agent';
import { createMessage, createMessageWithAction } from '../utils/chatUtils';

export default {
  methods: {
    /**
     * 处理流式响应
     * @param {string} messageToSend - 发送给 AI 的消息
     * @param {string} userContent - 用户原始消息内容
     */
    async handleStreamResponse(messageToSend, userContent) {
      console.log('发送消息给 AI:', messageToSend.substring(0, 100) + '...');
      console.log('请求参数:', {
        user_id: this.userId,
        session_id: this.sessionId,
        message_length: messageToSend.length
      });
      
      // 工具执行步骤
      let toolSteps = [];
      
      // 创建一个空的消息占位符（不显示，等有了内容再添加）
      let assistantMsg = null;
      let assistantMessage = '';
      let actionType = null;
      let actionData = null;
      
      await agentApi.chatStream({
        user_id: this.userId,
        session_id: this.sessionId,
        message: messageToSend,
        onChunk: (chunk) => {
          this.handleChunk(chunk, assistantMsg, assistantMessage, (newMsg, newContent) => {
            assistantMsg = newMsg;
            assistantMessage = newContent;
          });
        },
        onAction: (action) => {
          console.log('收到 action 数据:', action);
          actionType = action.action_type;
          // 将 JSON 字符串解析为对象
          try {
            actionData = typeof action.action_data === 'string' 
              ? JSON.parse(action.action_data) 
              : action.action_data;
          } catch (e) {
            console.error('解析 action_data 失败:', e);
            actionData = action.action_data;
          }
        },
        onToolStatus: (toolStatus) => {
          toolSteps = this.handleToolStatus(toolStatus, toolSteps, assistantMsg);
        },
      });
      
      console.log('AI 返回:', { assistantMessage, actionType, actionData });
      
      // 重要：清理已显示消息中的 [ACTION:xxx] 标记（全局清理）
      this.messages.forEach((msg, index) => {
        if (msg.content && typeof msg.content === 'string') {
          // 移除所有 [ACTION:xxx] 标记
          const cleanContent = msg.content.replace(/\[ACTION:\w+\]/g, '').trim();
          if (cleanContent !== msg.content) {
            // 内容发生变化，使用 $set 更新
            this.$set(this.messages, index, { ...msg, content: cleanContent });
          }
        }
      });
      
      // 移除加载状态（通过查找 role='loading'的消息）
      const loadingIndex = this.messages.findIndex(m => m.role === 'loading');
      if (loadingIndex !== -1) {
        this.messages.splice(loadingIndex, 1);
      }
      this.loading = false;
      
      // 移除图片预览（在发送成功后）
      if (this.showImagePreview && this.scanImage) {
        this.removeImage();
      }
      
      // 如果有 action 数据，需要更新消息
      if (actionType && actionData) {
        await this.processActionResponse(actionType, actionData, assistantMsg, assistantMessage, userContent);
      } else {
        // 检查 tool 返回结果中的 pending_confirmation 标记
        await this.checkSpecialActions(assistantMessage, userContent);
      }
      
      this.scrollToBottom();
      
      // 保存到本地存储
      this.saveMessagesToStorage();
    },
    
    /**
     * 处理流式响应块
     */
    handleChunk(chunk, assistantMsg, assistantMessage, updateCallback) {
      // 每收到一个字就追加到消息中
      assistantMessage += chunk;
      
      // 如果是第一个字，创建消息气泡
      if (!assistantMsg) {
        assistantMsg = createMessage('assistant', chunk);
        this.messages.push(assistantMsg);
        this.scrollToBottom();
      } else {
        // 更新消息内容：通过替换整个数组元素来强制 Vue 重新渲染
        const index = this.messages.indexOf(assistantMsg);
        if (index !== -1) {
          // 创建一个新的消息对象，强制触发响应式更新
          const newMsg = Object.assign({}, assistantMsg, { content: assistantMessage });
          this.messages.splice(index, 1, newMsg);
          assistantMsg = newMsg;
        }
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }
      
      // 更新回调
      if (updateCallback) {
        updateCallback(assistantMsg, assistantMessage);
      }
    },
    
    /**
     * 处理工具状态
     */
    handleToolStatus(toolStatus, toolSteps, assistantMsg) {
      console.log('收到工具状态:', toolStatus);
      
      if (toolStatus.type === 'tool_start') {
        // 添加工具步骤
        toolSteps.push({
          tool_name: toolStatus.tool_name,
          description: toolStatus.description,
          status: 'processing',
          error: null
        });
        
        // 如果还没有消息气泡，先创建一个
        if (!assistantMsg) {
          // 移除加载状态消息
          const loadingIndex = this.messages.findIndex(m => m.role === 'loading');
          if (loadingIndex !== -1) {
            this.messages.splice(loadingIndex, 1);
          }
          
          // 创建消息气泡显示工具执行步骤
          // 使用工具描述作为消息内容，这样用户能看到正在执行什么操作
          assistantMsg = createMessage('assistant', toolStatus.description + '...');
          assistantMsg.toolSteps = toolSteps;
          this.messages.push(assistantMsg);
          this.scrollToBottom();
        } else {
          // 更新工具步骤：通过替换整个数组元素来强制 Vue 重新渲染
          const index = this.messages.indexOf(assistantMsg);
          if (index !== -1) {
            const newMsg = Object.assign({}, assistantMsg, { toolSteps: [...toolSteps] });
            this.messages.splice(index, 1, newMsg);
            assistantMsg = newMsg;
          }
        }
        
      } else if (toolStatus.type === 'tool_complete') {
        // 更新工具步骤状态
        const step = toolSteps.find(s => s.tool_name === toolStatus.tool_name);
        if (step) {
          step.status = toolStatus.status;
          if (toolStatus.status === 'error') {
            step.error = toolStatus.error || '执行失败';
          }
          
          // 强制更新视图
          if (assistantMsg) {
            const index = this.messages.indexOf(assistantMsg);
            if (index !== -1) {
              const newMsg = Object.assign({}, assistantMsg, { toolSteps: [...toolSteps] });
              this.messages.splice(index, 1, newMsg);
              assistantMsg = newMsg;
            }
          }
        }
      }
      
      return toolSteps;
    },
    
    /**
     * 处理 action 响应
     */
    async processActionResponse(actionType, actionData, assistantMsg, assistantMessage, userContent) {
      // 根据 action 类型确定提示文本
      let promptText = assistantMessage;
      if (!promptText || promptText.trim() === '') {
        // 如果 AI 没有返回文本，使用默认提示
        if (actionType === 'medicine') {
          promptText = '请确认是否添加以下药品到药箱：';
        } else if (actionType === 'plan') {
          promptText = '请确认是否创建以下用药计划：';
        } else if (actionType === 'task') {
          promptText = '请确认是否更新以下用药任务：';
        }
      }
      
      // 包装 actionData，确保有正确的结构
      let wrappedActionData;
      if (actionType === 'medicine') {
        // 药箱卡片需要 { data: {...}, status: 'pending' }
        wrappedActionData = {
          data: actionData,
          status: 'pending'
        };
      } else if (actionType === 'plan') {
        // 用药计划卡片需要 { data: {...}, showConfirm: true, showEdit: true }
        wrappedActionData = {
          data: actionData,
          showConfirm: true,
          showEdit: true
        };
      } else if (actionType === 'task') {
        // 用药任务卡片需要 { data: {...}, status: 'pending' }
        wrappedActionData = {
          data: actionData,
          status: 'pending'
        };
      } else {
        // 其他类型，直接使用
        wrappedActionData = actionData;
      }
      
      // 直接更新已有消息的 actionType 和 actionData（不删除消息）
      if (assistantMsg) {
        const msgIndex = this.messages.findIndex(m => m.id === assistantMsg.id);
        if (msgIndex !== -1) {
          // 使用 $set 确保响应式更新
          this.$set(this.messages[msgIndex], 'content', promptText);
          this.$set(this.messages[msgIndex], 'actionType', actionType);
          this.$set(this.messages[msgIndex], 'actionData', wrappedActionData);
          this.$set(this.messages[msgIndex], 'type', 'action');
        }
      } else {
        // 如果没有消息，创建一个新的
        const assistantMsgWithAction = createMessageWithAction(
          'assistant',
          promptText,
          actionType,
          wrappedActionData
        );
        this.messages.push(assistantMsgWithAction);
      }
      
      this.scrollToBottom();
    },
    
    /**
     * 检查特殊 action 标记
     */
    async checkSpecialActions(assistantMessage, userContent) {
      // 检测特殊标记：[ACTION:plan_confirm], [ACTION:plan_update], [ACTION:plan_delete], [ACTION:addMedicine], [ACTION:updateTaskStatus]
      // 注意：需要捕获 [ACTION:xxx] 之前的内容
      const actionMatch = assistantMessage?.match(/([\s\S]*?)\[ACTION:(plan_\w+|addMedicine|updateTaskStatus)\]/);
      
      if (actionMatch) {
        const actionType = actionMatch[2]; // plan_confirm, plan_update, plan_delete, addMedicine, updateTaskStatus
        const actionData = actionMatch[1].trim(); // [ACTION:xxx] 之前的内容
        
        console.log('检测到特殊标记:', actionType, actionData);
        
        if (actionType === 'addMedicine') {
          // 显示药箱确认卡片（不显示原始消息，因为包含 [ACTION:addMedicine] 标记）
          const medicineData = {
            data: {
              medicineName: this.extractMedicineName(assistantMessage) || '',
              defaultDosage: this.extractDosage(assistantMessage) || '',
              remark: ''
            },
            status: 'pending'
          };
          
          const assistantMsg = createMessageWithAction(
            'assistant',
            '请确认是否添加以下药品到药箱：',
            'medicine',
            medicineData
          );
          this.messages.push(assistantMsg);
          this.scrollToBottom();
          
          // 重要：清空 assistantMessage，避免显示 [ACTION:addMedicine] 标记
          assistantMessage = '';
          
          // 同时更新已显示的消息，移除 [ACTION:addMedicine] 标记
          if (assistantMsg) {
            const msgIndex = this.messages.findIndex(m => m.id === assistantMsg.id);
            if (msgIndex !== -1) {
              // 移除消息中的 [ACTION:addMedicine] 标记
              const cleanContent = assistantMsg.content.replace(/\[ACTION:addMedicine\]/g, '').trim();
              this.$set(this.messages[msgIndex], 'content', cleanContent || '请确认是否添加以下药品到药箱：');
            }
          }
        } else if (actionType === 'updateTaskStatus') {
          // 显示任务确认卡片
          const taskData = {
            data: {
              taskId: null,
              medicineName: this.extractMedicineName(assistantMessage) || '',
              timePoint: this.extractTimePoint(assistantMessage) || '',
              dosage: this.extractDosage(assistantMessage) || '',
              status: 1
            },
            status: 'pending'
          };
          
          const assistantMsg = createMessageWithAction(
            'assistant',
            '请确认是否更新此用药任务状态：',
            'task',
            taskData
          );
          this.messages.push(assistantMsg);
          this.scrollToBottom();
        } else {
          // plan 相关操作
          // 解析详情数据
          const planData = this.parseActionData(actionData);
          
          // 显示确认卡片
          const assistantMsg = createMessageWithAction(
            'assistant',
            '请确认以下操作：',
            'plan',
            {
              data: planData,
              showConfirm: true,
              showEdit: true
            }
          );
          this.messages.push(assistantMsg);
        }
      } else {
        // 检测用户意图：如果用户想创建计划，但 AI 直接创建了（没有显示卡片），则显示确认卡片
        const contentSafe = userContent || '';
        const userWantsToCreatePlan = contentSafe.toLowerCase().includes('创建') && 
          (contentSafe.toLowerCase().includes('计划') || contentSafe.toLowerCase().includes('用药'));
        
        const aiAlreadyCreatedPlan = assistantMessage && 
          (assistantMessage.includes('已创建') || assistantMessage.includes('创建成功') || 
           assistantMessage.includes('plan created'));
        
        if (userWantsToCreatePlan && aiAlreadyCreatedPlan) {
          console.log('检测到 AI 直接创建了计划，显示确认卡片');
          // 显示确认卡片，让用户可以编辑或确认
          const planData = {
            data: {
              medicineName: this.extractMedicineName(userContent) || '',
              timePoint: '08:00',
              dosage: '',
              frequency: '每日一次',
              startDate: new Date().toISOString().split('T')[0]
            },
            showConfirm: true,
            showEdit: true
          };
          
          const assistantMsg = createMessageWithAction(
            'assistant',
            assistantMessage + '\n\n请确认以上信息是否正确，如有需要可以修改：',
            'plan',
            planData
          );
          this.messages.push(assistantMsg);
        } else if (assistantMessage && (assistantMessage.includes('为您准备') || assistantMessage.includes('请确认'))) {
          // 显示空白卡片让用户填写
          const planData = {
            data: {
              medicineName: '',
              timePoint: '08:00',
              dosage: '',
              frequency: '每日一次',
              startDate: new Date().toISOString().split('T')[0]
            },
            showConfirm: true,
            showEdit: true
          };
          
          const assistantMsg = createMessageWithAction(
            'assistant',
            assistantMessage,
            'plan',
            planData
          );
          this.messages.push(assistantMsg);
        } else {
          // 普通文本消息已经在 onChunk 中添加了，这里不需要重复添加
          console.log('普通消息已在流式中显示');
        }
      }
    }
  }
};
