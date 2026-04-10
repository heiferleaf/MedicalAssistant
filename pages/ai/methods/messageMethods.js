/**
 * 消息处理方法模块
 * 封装消息存储、加载、滚动等相关方法
 */

import { StorageKeys, getFromStorage, setToStorage } from '../utils/chatUtils';

export default {
  methods: {
    /**
     * 保存消息到本地存储
     */
    saveMessagesToStorage() {
      if (!this.sessionId) return;
      
      const storageKey = `${StorageKeys.MESSAGES}_${this.sessionId}`;
      
      // 过滤掉 loading 状态的消息
      const messagesToSave = this.messages.filter(msg => msg.type !== 'loading');
      
      setToStorage(storageKey, messagesToSave);
      console.log('消息已保存到本地存储:', messagesToSave.length);
    },
    
    /**
     * 从本地存储加载消息
     * @param {string} sessionId - 会话 ID
     * @returns {Array|null} 消息列表
     */
    loadMessagesFromStorage(sessionId) {
      if (!sessionId) return null;
      
      const storageKey = `${StorageKeys.MESSAGES}_${sessionId}`;
      const localMessages = getFromStorage(storageKey);
      
      if (localMessages && Array.isArray(localMessages)) {
        console.log('从本地存储加载消息:', localMessages.length);
        
        // 修复消息数据
        return localMessages.map(msg => this.repairMessageData(msg));
      }
      
      return null;
    },
    
    /**
     * 修复消息数据（图片、actionData 等）
     * @param {object} msg - 消息对象
     * @returns {object} 修复后的消息
     */
    repairMessageData(msg) {
      // 修复图片消息
      if (msg.type === 'image' && msg.imagePath) {
        if (msg.imagePath.startsWith('blob:')) {
          console.warn('图片 URL 已失效:', msg.imagePath.substring(0, 50) + '...');
          msg.imagePath = '';
        } else if (!msg.imagePath.startsWith('data:') && !msg.imagePath.startsWith('http')) {
          if (msg.imagePath.startsWith('/images/')) {
            const fileName = msg.imagePath.split('/').pop();
            const base64 = uni.getStorageSync('drug_image_' + fileName);
            if (base64) {
              msg.imagePath = `data:image/jpeg;base64,${base64}`;
            } else {
              console.warn('localStorage 中未找到图片数据:', fileName);
              msg.imagePath = '';
            }
          } else {
            console.log('检测到纯 Base64 数据，添加前缀');
            msg.imagePath = `data:image/jpeg;base64,${msg.imagePath}`;
          }
        }
      }
      
      // 修复 actionData 结构
      if (msg.actionType && msg.actionData) {
        try {
          const actionData = typeof msg.actionData === 'string' 
            ? JSON.parse(msg.actionData) 
            : msg.actionData;
          
          // 检查是否已经包装过（有 data 字段）
          if (!actionData.data) {
            // 未包装，需要包装
            let wrappedActionData;
            if (msg.actionType === 'medicine') {
              wrappedActionData = {
                data: actionData,
                status: 'pending'
              };
            } else if (msg.actionType === 'plan') {
              wrappedActionData = {
                data: actionData,
                showConfirm: true,
                showEdit: true
              };
            } else if (msg.actionType === 'task') {
              wrappedActionData = {
                data: actionData,
                status: 'pending'
              };
            } else {
              wrappedActionData = actionData;
            }
            
            // 更新消息的 actionData
            msg.actionData = wrappedActionData;
          }
        } catch (e) {
          console.error('解析 actionData 失败:', e);
        }
      }
      
      return msg;
    },
    
    /**
     * 滚动到底部
     */
    scrollToBottom() {
      this.$nextTick(() => {
        uni.createSelectorQuery()
          .select('#chat-view')
          .boundingClientRect()
          .exec();
        
        // 通知 ChatView 组件滚动
        if (this.$refs.chatView && this.$refs.chatView.scrollToBottom) {
          this.$refs.chatView.scrollToBottom();
        }
      });
    }
  }
};
