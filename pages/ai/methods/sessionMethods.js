/**
 * 会话管理方法模块
 * 封装所有会话相关的操作方法
 */

import sessionApi from '@/api/session';
import agentApi from '@/api/agent';
import { StorageKeys, getFromStorage } from '../utils/chatUtils';

export default {
  methods: {
    /**
     * 加载会话列表
     */
    async loadSessions() {
      try {
        const res = await sessionApi.getSessions(this.userId);
        
        if (res && Array.isArray(res)) {
          this.sessions = res;
          
          if (res.length > 0) {
            this.switchSession(res[0]);
          } else {
            this.createNewSession();
          }
        }
      } catch (error) {
        console.error('加载会话列表失败:', error);
      }
    },
    
    /**
     * 创建新会话
     */
    async createNewSession() {
      try {
        const res = await sessionApi.createSession(this.userId);
        
        if (res && res.sessionId) {
          const newSession = {
            sessionId: res.sessionId,
            userId: this.userId,
            summary: '新会话',
            lastMessage: '开始新的对话',
            createdAt: new Date().toISOString(),
            needsNaming: true  // 标记需要命名
          };
          
          this.sessions.unshift(newSession);
          this.switchSession(newSession);
          this.showSidebar = false;
          
          // 清除待确认请求（避免影响新会话）
          try {
            await agentApi.deleteAllPending(this.userId);
            console.log('已删除所有待确认请求');
          } catch (cleanupError) {
            console.error('删除待确认请求失败:', cleanupError);
          }
          
          // 初始化本地存储（空消息列表）
          this.saveMessagesToStorage();
        }
      } catch (error) {
        console.error('创建会话失败:', error);
        uni.showToast({
          title: '创建会话失败',
          icon: 'none'
        });
      }
    },
    
    /**
     * 切换会话
     * @param {object} session - 会话对象
     */
    async switchSession(session) {
      this.currentSessionId = session.sessionId;
      this.sessionId = session.sessionId;
      
      // 清除待确认请求（避免影响新会话）
      try {
        await agentApi.deleteAllPending(this.userId);
        console.log('已删除所有待确认请求');
      } catch (cleanupError) {
        console.error('删除待确认请求失败:', cleanupError);
      }
      
      // 先从本地存储加载消息
      const storageKey = `${StorageKeys.MESSAGES}_${this.sessionId}`;
      const localMessages = getFromStorage(storageKey);
      
      if (localMessages && Array.isArray(localMessages)) {
        // 有本地缓存，直接使用
        console.log('从本地存储加载消息:', localMessages.length);
        
        // 修复图片消息的 imagePath 和 actionData 结构
        this.messages = localMessages.map(msg => {
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
        });
        
        this.scrollToBottom();
      } else {
        // 没有本地缓存，从后端加载
        this.loadMessagesFromBackend(this.sessionId);
      }
    },
    
    /**
     * 从后端加载消息历史
     * @param {string} sessionId - 会话 ID
     */
    async loadMessagesFromBackend(sessionId) {
      try {
        const res = await sessionApi.getMessages(sessionId);
        
        if (res && Array.isArray(res)) {
          this.messages = res.map(msg => {
            // 如果有 actionType，说明是带卡片的消息
            if (msg.actionType && msg.actionData) {
              try {
                const actionData = typeof msg.actionData === 'string' 
                  ? JSON.parse(msg.actionData) 
                  : msg.actionData;
                              
                // 包装 actionData，确保有正确的结构
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
                              
                return this.createMessageWithAction(
                  msg.role,
                  msg.content,
                  msg.actionType,
                  wrappedActionData
                );
              } catch (e) {
                console.error('解析 actionData 失败:', e);
                return this.createMessage(msg.role, msg.content);
              }
            } else {
              return this.createMessage(msg.role, msg.content, 'text', {
                createdAt: msg.createdAt
              });
            }
          });
          
          this.scrollToBottom();
          
          // 保存到本地存储
          this.saveMessagesToStorage();
        }
      } catch (error) {
        console.error('加载消息历史失败:', error);
        this.messages = [];
      }
    },
    
    /**
     * 删除会话
     * @param {object} session - 会话对象
     * @param {number} index - 会话在列表中的索引
     */
    async deleteSession(session, index) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这个会话吗？删除后无法恢复。',
        success: async (res) => {
          if (res.confirm) {
            try {
              await sessionApi.deleteSession(session.sessionId);
              this.sessions.splice(index, 1);
              
              if (this.currentSessionId === session.sessionId) {
                if (this.sessions.length > 0) {
                  this.switchSession(this.sessions[0]);
                } else {
                  this.messages = [];
                  this.createNewSession();
                }
              }
              
              uni.showToast({
                title: '删除成功',
                icon: 'success'
              });
            } catch (error) {
              console.error('删除会话失败:', error);
              uni.showToast({
                title: '删除失败',
                icon: 'none'
              });
            }
          }
        }
      });
    },
    
    /**
     * 切换侧边栏显示状态
     */
    toggleSidebar() {
      this.showSidebar = !this.showSidebar;
    },
    
    /**
     * 更新会话名称
     * @param {object} param0 - 参数对象 { session, index, newName }
     */
    async handleUpdateSessionName({ session, index, newName }) {
      try {
        // 调用后端 API 更新会话名称
        await sessionApi.updateSession(session.sessionId, {
          summary: newName
        });
        
        // 更新本地数据
        this.sessions[index].summary = newName;
        
        uni.showToast({
          title: '修改成功',
          icon: 'success'
        });
      } catch (error) {
        console.error('更新会话名称失败:', error);
        uni.showToast({
          title: '修改失败',
          icon: 'none'
        });
      }
    }
  }
};
