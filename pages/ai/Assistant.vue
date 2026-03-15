<template>
	<view class="chat-container">
		<view class="status-bar"></view>
		
		<!-- 会话侧边栏 -->
		<SessionSidebar
			:visible="showSidebar"
			:sessions="sessions"
			:currentSessionId="currentSessionId"
			@new="createNewSession"
			@close="toggleSidebar"
			@switch="switchSession"
			@delete="deleteSession"
		/>
		
		<!-- 主聊天区域 -->
		<view class="main-content">
			<!-- 聊天头部 -->
			<ChatHeader
				:showMenu="true"
				@toggle-sidebar="toggleSidebar"
			/>
			
			<!-- 聊天视图 -->
			<ChatView
				:messages="messages"
				:scrollToMsgId="scrollToMsgId"
				:loading="loading"
				@load-more="loadMoreMessages"
				@scroll-to="scrollToMessage"
			/>
			
			<!-- 底部输入区 -->
			<view class="footer">
				<!-- 快捷短语 -->
				<ShortcutBar
					:shortcuts="shortcuts"
					@click="handleShortcut"
				/>
				
				<!-- 输入框 -->
				<ChatInput
					@send="sendMessage"
					@add="handleAdd"
					@voice="handleVoice"
				/>
			</view>
		</view>
	</view>
</template>

<script>
// 导入组件
import ChatHeader from './components/ChatHeader.vue';
import ChatView from './views/ChatView.vue';
import ChatInput from './components/ChatInput.vue';
import ShortcutBar from './components/ShortcutBar.vue';
import SessionSidebar from './components/SessionSidebar.vue';

// 导入工具函数
import { 
	createMessage, 
	defaultShortcuts, 
	StorageKeys,
	getFromStorage,
	setToStorage,
	generateSessionId 
} from './utils/chatUtils';

// 导入 API
import agentApi from '@/api/agent';
import sessionApi from '@/api/session';

export default {
	name: 'Assistant',
	components: {
		ChatHeader,
		ChatView,
		ChatInput,
		ShortcutBar,
		SessionSidebar
	},
	data() {
		return {
			messages: [],
			sessions: [],
			currentSessionId: '',
			sessionId: '',
			userId: '',
			shortcuts: defaultShortcuts,
			showSidebar: false,
			scrollToMsgId: '',
			loading: false
		}
	},
	onLoad() {
		this.init();
	},
	methods: {
		// 初始化
		async init() {
			this.userId = getFromStorage(StorageKeys.USER_ID);
			
			if (!this.userId) {
				uni.showToast({
					title: '请先登录',
					icon: 'none'
				});
				setTimeout(() => {
					uni.reLaunch({ url: '/pages/Login/Login' });
				}, 1500);
				return;
			}
			
			await this.loadSessions();
		},
		
		// 加载会话列表
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
		
		// 创建新会话
		async createNewSession() {
			try {
				const res = await sessionApi.createSession(this.userId);
				
				if (res && res.sessionId) {
					const newSession = {
						sessionId: res.sessionId,
						userId: this.userId,
						summary: '新会话',
						lastMessage: '开始新的对话',
						createdAt: new Date().toISOString()
					};
					
					this.sessions.unshift(newSession);
					this.switchSession(newSession);
					this.showSidebar = false;
				}
			} catch (error) {
				console.error('创建会话失败:', error);
				uni.showToast({
					title: '创建会话失败',
					icon: 'none'
				});
			}
		},
		
		// 切换会话
		async switchSession(session) {
			this.currentSessionId = session.sessionId;
			this.sessionId = session.sessionId;
			
			try {
				const res = await sessionApi.getMessages(this.sessionId);
				
				if (res && Array.isArray(res)) {
					this.messages = res.map(msg => 
						createMessage(msg.role, msg.content, 'text', {
							createdAt: msg.createdAt
						})
					);
					
					this.scrollToBottom();
				}
			} catch (error) {
				console.error('加载消息历史失败:', error);
				this.messages = [];
			}
		},
		
		// 删除会话
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
		
		// 切换侧边栏
		toggleSidebar() {
			this.showSidebar = !this.showSidebar;
		},
		
		// 发送消息
		async sendMessage(content) {
			// 添加用户消息
			const userMsg = createMessage('user', content);
			this.messages.push(userMsg);
			this.scrollToBottom();
			
			// 添加加载状态
			const loadingMsg = createMessage('loading', '');
			this.messages.push(loadingMsg);
			this.scrollToBottom();
			this.loading = true;
			
			try {
				const response = await agentApi.chat({
					user_id: this.userId,
					session_id: this.sessionId,
					message: content,
					type: 'chat'
				});
				
				// 移除加载状态
				this.messages.pop();
				
				// 添加 AI 回复
				let assistantMessage = response?.assistant_message || response?.message || response?.content || '收到您的消息，我正在思考中...';
				const assistantMsg = createMessage('assistant', assistantMessage);
				this.messages.push(assistantMsg);
				this.scrollToBottom();
				
				// 更新会话预览
				this.updateSessionPreview(content, assistantMessage);
				
			} catch (error) {
				console.error('请求失败:', error);
				this.messages.pop();
				const errorMsg = createMessage('assistant', '抱歉，网络开小差了，请稍后再试。');
				this.messages.push(errorMsg);
				this.scrollToBottom();
			} finally {
				this.loading = false;
			}
		},
		
		// 处理快捷短语
		handleShortcut(shortcut) {
			this.sendMessage(shortcut.text);
		},
		
		// 处理添加按钮
		handleAdd() {
			console.log('点击添加按钮');
			// TODO: 实现添加功能
		},
		
		// 处理语音按钮
		handleVoice() {
			console.log('点击语音按钮');
			// TODO: 实现语音功能
		},
		
		// 加载更多消息
		loadMoreMessages() {
			console.log('加载更多消息');
			// TODO: 实现分页加载
		},
		
		// 滚动到指定消息
		scrollToMessage(msgId) {
			this.scrollToMsgId = msgId;
		},
		
		// 滚动到底部
		scrollToBottom() {
			if (this.messages.length > 0) {
				this.scrollToMsgId = 'msg-' + (this.messages.length - 1);
			}
		},
		
		// 更新会话预览
		updateSessionPreview(userMessage, assistantMessage) {
			const session = this.sessions.find(s => s.sessionId === this.sessionId);
			if (session) {
				session.lastMessage = assistantMessage;
				session.summary = userMessage.substring(0, 20);
				
				// 移到列表顶部
				const index = this.sessions.indexOf(session);
				if (index > 0) {
					this.sessions.splice(index, 1);
					this.sessions.unshift(session);
				}
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.chat-container {
	height: 100vh;
	display: flex;
	flex-direction: row;
	background-color: #F1F5F9;
	overflow: hidden;
	@media (prefers-color-scheme: dark) { background-color: #020617; }
}

.status-bar { 
	height: var(--status-bar-height); 
	background: #fff; 
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	z-index: 1000;
	@media (prefers-color-scheme: dark) { background: #0F172A; } 
}

.main-content {
	flex: 1;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.footer {
	padding: 20rpx 30rpx;
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: blur(20px);
	border-top: 1rpx solid #e2e8f0;
	@media (prefers-color-scheme: dark) { 
		background: rgba(15, 23, 42, 0.9); 
		border-color: #1e293b; 
	}
}
</style>
