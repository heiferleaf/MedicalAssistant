<template>
	<view class="chat-container">
		<view class="status-bar"></view>
		
		<!-- 侧边栏：会话列表 -->
		<view v-if="showSidebar" class="sidebar">
			<view class="sidebar-header">
				<text class="sidebar-title">会话列表</text>
				<view class="sidebar-actions">
					<view class="action-btn" @click="createNewSession">
						<text class="btn-text">+</text>
					</view>
					<view class="action-btn" @click="toggleSidebar">
						<text class="btn-text">×</text>
					</view>
				</view>
			</view>
			<scroll-view scroll-y class="session-list">
				<view 
					v-for="(session, index) in sessions" 
					:key="index" 
					:class="['session-item', session.sessionId === currentSessionId ? 'active' : '']"
					@click="switchSession(session.sessionId)"
				>
					<view class="session-info">
						<text class="session-title">{{ session.summary || '新会话' }}</text>
						<text class="session-preview">{{ session.lastMessage || '暂无消息' }}</text>
					</view>
					<view class="session-delete" @click.stop="deleteSession(session.sessionId, index)">
						<text>🗑️</text>
					</view>
				</view>
			</scroll-view>
		</view>

		<!-- 主聊天区域 -->
		<view class="main-content">
			<view class="header">
				<view class="header-left">
					<view class="menu-btn" @click="toggleSidebar">
						<text class="menu-icon">☰</text>
					</view>
					<view class="ai-avatar">AI</view>
					<view class="header-info">
						<text class="header-title">智愈助手</text>
						<view class="online-status">
							<view class="dot"></view>
							<text class="status-text">在线 | 为您的健康护航</text>
						</view>
					</view>
				</view>
			</view>

			<scroll-view scroll-y class="chat-main" :scroll-into-view="scrollToMsgId" scroll-with-animation>
				<view class="chat-content">
					<view v-for="(msg, index) in messages" :key="index" :id="'msg-' + index">
						<view v-if="msg.role === 'assistant'" class="ai-msg-group">
							<view class="chat-bubble-ai card-shadow">
								<text v-if="msg.type === 'text'" class="msg-text-bold">{{ msg.content }}</text>
								
								<view v-if="msg.type === 'briefing'" class="briefing-box">
									<view v-for="(item, i) in msg.briefingItems" :key="i" class="brief-item">
										<text class="brief-label">{{ item.label }}</text>
										<text :class="['brief-val', item.class]">{{ item.value }}</text>
									</view>
								</view>
								
								<text v-if="msg.type === 'advice'" class="advice">{{ msg.content }}</text>
								
								<view v-if="msg.type === 'result'" class="result-section">
									<view class="result-header">
										<text class="result-title">{{ msg.resultTitle }}</text>
										<image class="icon" src="../../static/Home/check.svg"/>
										<view v-if="msg.resultTag" class="result-tag">{{ msg.resultTag }}</view>
									</view>
									<text class="msg-text">{{ msg.content }}</text>
									<view v-if="msg.actions" class="action-btns">
										<button v-for="(action, i) in msg.actions" :key="i" :class="action.class + '-btn'">{{ action.text }}</button>
									</view>
								</view>
								
								<view v-if="msg.type === 'chart'" class="chart-section">
									<view class="chart-header">
										<view>
											<text class="chart-title">{{ msg.chartTitle }}</text>
											<text class="chart-unit">{{ msg.chartUnit }}</text>
										</view>
										<view v-if="msg.liveTag" class="live-tag">{{ msg.liveTag }}</view>
									</view>
									<view class="chart-container-inner">
										<svg class="chart-svg" viewBox="0 0 200 60">
											<path d="M0 45 Q 40 40, 60 50 T 100 45 T 140 35 T 200 40" fill="none" stroke="#3B82F6" stroke-width="2"></path>
											<circle cx="140" cy="35" fill="#3B82F6" r="3"></circle>
										</svg>
										<view v-if="msg.chartPopover" class="chart-popover">
											<text class="pop-val">{{ msg.chartPopover.value }}</text>
											<text class="pop-time">{{ msg.chartPopover.time }}</text>
										</view>
									</view>
									<view v-if="msg.chartActions" class="chart-actions">
										<view v-for="(action, i) in msg.chartActions" :key="i" :class="action.class">{{ action.text }}</view>
									</view>
								</view>
							</view>
						</view>
						
						<view v-else-if="msg.role === 'user'" class="user-msg-group">
							<view class="chat-bubble-user card-shadow">
								<image v-if="msg.image" class="msg-img" :src="msg.image" mode="aspectFill"></image>
								<text class="user-text">{{ msg.content }}</text>
							</view>
						</view>
						
						<view v-else-if="msg.role === 'loading'" class="ai-msg-group">
							<view class="chat-bubble-ai card-shadow">
								<view class="loading-dots">
									<view class="dot-loading"></view>
									<view class="dot-loading"></view>
									<view class="dot-loading"></view>
								</view>
							</view>
						</view>
					</view>
				</view>
			</scroll-view>

			<view class="footer">
				<scroll-view scroll-x class="shortcut-scroll" show-scrollbar="false">
					<view v-for="(shortcut, index) in shortcuts" :key="index" class="shortcut-item" @click="sendShortcut(shortcut)">
						<image class="icon" :src="shortcut.icon"/>
						<text>{{ shortcut.text }}</text>
					</view>
				</scroll-view>

				<view class="input-row">
					<image class="icon" src="../../static/Health/plus-circle.svg"/>
					<view class="input-box">
						<input 
							v-model="inputMessage" 
							class="real-input" 
							placeholder="输入健康疑问..." 
							placeholder-class="ph"
							@confirm="sendMessage"
						/>
						<image class="icon" src="../../static/ai/microphone.svg"/>
					</view>
					<view class="send-btn" @click="sendMessage">
						<image class="icon" src="../../static/ai/send.svg"/>
					</view>
				</view>
				<view class="home-bar"></view>
			</view>
		</view>
	</view>
</template>

<script>
import agentApi from '@/api/agent'
import sessionApi from '@/api/session'

export default {
	data() {
		return {
			inputMessage: '',
			scrollToMsgId: '',
			shortcuts: [
				{ icon: '../../static/Health/pill-active.svg', text: '询问副作用' },
				{ icon: '../../static/Home/warning.svg', text: '报告不适' },
				{ icon: '../../static/Mine/report.svg', text: '就医清单' }
			],
			messages: [],
			sessionId: '',
			currentSessionId: '',
			sessions: [],
			showSidebar: false,
			userId: ''
		}
	},
	onLoad() {
		this.userId = uni.getStorageSync('userId');
		console.log('初始化 AI 助手，userId:', this.userId);
		
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
		
		this.loadSessions();
	},
	methods: {
		async loadSessions() {
			try {
				const res = await sessionApi.getSessions(this.userId);
				console.log('获取会话列表:', res);
				
				if (res && Array.isArray(res)) {
					this.sessions = res;
					
					if (res.length > 0) {
						this.switchSession(res[0].sessionId);
					} else {
						this.createNewSession();
					}
				}
			} catch (error) {
				console.error('加载会话列表失败:', error);
			}
		},
		
		async createNewSession() {
			try {
				const res = await sessionApi.createSession(this.userId);
				console.log('创建新会话:', res);
				
				if (res && res.sessionId) {
					const newSession = {
						sessionId: res.sessionId,
						userId: this.userId,
						summary: '新会话',
						lastMessage: '开始新的对话',
						createdAt: new Date().toISOString()
					};
					
					this.sessions.unshift(newSession);
					this.switchSession(res.sessionId);
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
		
		async switchSession(sessionId) {
			console.log('切换会话:', sessionId);
			this.currentSessionId = sessionId;
			this.sessionId = sessionId;
			
			try {
				const res = await sessionApi.getMessages(sessionId);
				console.log('获取消息历史:', res);
				
				if (res && Array.isArray(res)) {
					this.messages = res.map(msg => {
						console.log('消息项:', msg);
						return {
							role: msg.role,
							type: 'text',
							content: msg.content,
							createdAt: msg.createdAt
						};
					});
					
					console.log('处理后的消息列表:', this.messages);
					this.scrollToMsgId = 'msg-' + (this.messages.length - 1);
				} else {
					this.messages = [];
				}
			} catch (error) {
				console.error('加载消息历史失败:', error);
				this.messages = [];
			}
		},
		
		async deleteSession(sessionId, index) {
			uni.showModal({
				title: '确认删除',
				content: '确定要删除这个会话吗？删除后无法恢复。',
				success: async (res) => {
					if (res.confirm) {
						try {
							await sessionApi.deleteSession(sessionId);
							this.sessions.splice(index, 1);
							
							if (this.currentSessionId === sessionId) {
								if (this.sessions.length > 0) {
									this.switchSession(this.sessions[0].sessionId);
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
		
		toggleSidebar() {
			this.showSidebar = !this.showSidebar;
		},
		
		async sendMessage() {
			if (!this.inputMessage.trim()) return
			
			const userMessage = {
				role: 'user',
				content: this.inputMessage.trim()
			}
			
			this.messages.push(userMessage)
			this.scrollToMsgId = 'msg-' + (this.messages.length - 1)
			
			const userInput = this.inputMessage
			this.inputMessage = ''
			
			this.messages.push({ role: 'loading' })
			this.scrollToMsgId = 'msg-' + (this.messages.length - 1)
			
			try {
				console.log('发送请求:', { 
					user_id: this.userId, 
					session_id: this.sessionId, 
					message: userInput 
				});
				
				const response = await agentApi.chat({
					user_id: this.userId,
					session_id: this.sessionId,
					message: userInput,
					type: 'chat'
				})
				
				console.log('收到响应:', response);
				this.messages.pop()
				
				let assistantMessage = '';
				if (response && response.assistant_message) {
					assistantMessage = response.assistant_message;
				} else if (response && response.message) {
					assistantMessage = response.message;
				} else if (response && response.content) {
					assistantMessage = response.content;
				} else {
					assistantMessage = '收到您的消息，我正在思考中...';
				}
				
				this.messages.push({
					role: 'assistant',
					type: 'text',
					content: assistantMessage
				})
				
				this.scrollToMsgId = 'msg-' + (this.messages.length - 1)
				
				// 更新会话列表中的最后一条消息
				this.updateSessionPreview(this.sessionId, userInput, assistantMessage);
				
			} catch (error) {
				console.error('请求失败:', error);
				this.messages.pop()
				this.messages.push({
					role: 'assistant',
					type: 'text',
					content: '抱歉，网络开小差了，请稍后再试。'
				})
				this.scrollToMsgId = 'msg-' + (this.messages.length - 1)
			}
		},
		
		updateSessionPreview(sessionId, userMessage, assistantMessage) {
			const session = this.sessions.find(s => s.sessionId === sessionId);
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
		},
		
		sendShortcut(shortcut) {
			this.inputMessage = shortcut.text
			this.sendMessage()
		}
	}
}
</script>

<style lang="scss" scoped>
// 基础变量与暗黑适配
$primary: #3B82F6;
$bg-light: #F8FAFC;
$bg-dark: #0F172A;

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
	@media (prefers-color-scheme: dark) { background: $bg-dark; } 
}

/* 侧边栏样式 */
.sidebar {
	width: 300px;
	height: 100vh;
	background: #fff;
	border-right: 1rpx solid #e2e8f0;
	display: flex;
	flex-direction: column;
	position: absolute;
	left: 0;
	top: 0;
	z-index: 999;
	@media (prefers-color-scheme: dark) { 
		background: #1e293b; 
		border-color: #334155;
	}
	
	.sidebar-header {
		padding: 30rpx;
		border-bottom: 1rpx solid #e2e8f0;
		display: flex;
		justify-content: space-between;
		align-items: center;
		@media (prefers-color-scheme: dark) { border-color: #334155; }
		
		.sidebar-title {
			font-size: 32rpx;
			font-weight: 700;
			color: #1e293b;
			@media (prefers-color-scheme: dark) { color: #f1f5f9; }
		}
		
		.sidebar-actions {
			display: flex;
			gap: 16rpx;
			
			.action-btn {
				width: 60rpx;
				height: 60rpx;
				background: $primary;
				border-radius: 50%;
				display: flex;
				align-items: center;
				justify-content: center;
				
				.btn-text {
					color: #fff;
					font-size: 32rpx;
					font-weight: bold;
				}
			}
		}
	}
	
	.session-list {
		flex: 1;
		overflow-y: auto;
		
		.session-item {
			padding: 30rpx;
			border-bottom: 1rpx solid #e2e8f0;
			display: flex;
			justify-content: space-between;
			align-items: center;
			cursor: pointer;
			transition: background 0.2s;
			@media (prefers-color-scheme: dark) { border-color: #334155; }
			
			&:active {
				background: #f1f5f9;
				@media (prefers-color-scheme: dark) { background: #334155; }
			}
			
			&.active {
				background: rgba(59, 130, 246, 0.1);
				border-left: 4rpx solid $primary;
			}
			
			.session-info {
				flex: 1;
				overflow: hidden;
				
				.session-title {
					font-size: 28rpx;
					font-weight: 600;
					color: #1e293b;
					display: block;
					margin-bottom: 8rpx;
					@media (prefers-color-scheme: dark) { color: #f1f5f9; }
				}
				
				.session-preview {
					font-size: 24rpx;
					color: #64748b;
					display: block;
					overflow: hidden;
					text-overflow: ellipsis;
					white-space: nowrap;
					@media (prefers-color-scheme: dark) { color: #94a3b8; }
				}
			}
			
			.session-delete {
				padding: 12rpx 20rpx;
				font-size: 28rpx;
				color: #ef4444;
				background: rgba(239, 68, 68, 0.1);
				border-radius: 8rpx;
				opacity: 0;
				transition: all 0.2s;
				display: flex;
				align-items: center;
				justify-content: center;
				font-weight: 600;
				
				&:active {
					opacity: 1;
					background: rgba(239, 68, 68, 0.2);
					transform: scale(1.05);
				}
				
				.session-item:hover & {
					opacity: 1;
				}
			}
		}
	}
}

/* 主内容区域 */
.main-content {
	flex: 1;
	display: flex;
	flex-direction: column;
	margin-left: 0;
	transition: margin-left 0.3s;
}

/* Header */
.header {
	padding: 20rpx 40rpx;
	background: rgba(255, 255, 255, 0.85);
	backdrop-filter: blur(20px);
	display: flex;
	justify-content: space-between;
	align-items: center;
	border-bottom: 1rpx solid #e2e8f0;
	z-index: 100;
	@media (prefers-color-scheme: dark) { 
		background: rgba(15, 23, 42, 0.85); 
		border-color: #1e293b; 
		color: #fff; 
	}

	.header-left {
		display: flex;
		align-items: center;
		gap: 20rpx;
		
		.menu-btn {
			width: 80rpx;
			height: 80rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			cursor: pointer;
			
			.menu-icon {
				font-size: 40rpx;
				color: #1e293b;
				@media (prefers-color-scheme: dark) { color: #f1f5f9; }
			}
		}
		
		.ai-avatar {
			width: 80rpx; 
			height: 80rpx; 
			background: $primary; 
			color: #fff;
			border-radius: 50%; 
			display: flex; 
			align-items: center; 
			justify-content: center;
			font-weight: bold; 
			font-size: 32rpx; 
			box-shadow: 0 8rpx 20rpx rgba(59, 130, 246, 0.2);
		}
		
		.header-title { 
			font-size: 34rpx; 
			font-weight: 700; 
		}
		
		.online-status {
			display: flex; 
			align-items: center; 
			gap: 8rpx;
			
			.dot { 
				width: 12rpx; 
				height: 12rpx; 
				background: #22c55e; 
				border-radius: 50%; 
			}
			
			.status-text { 
				font-size: 22rpx; 
				color: #16a34a; 
				font-weight: 500; 
			}
		}
	}
}

/* 聊天主体 */
.chat-main {
	flex: 1;
	height: 0;
	overflow-y: auto;
	
	.chat-content { 
		padding: 40rpx 30rpx; 
		padding-bottom: 60rpx; 
	}
}

.ai-msg-group, .user-msg-group { 
	margin-bottom: 40rpx; 
	display: flex; 
	flex-direction: column; 
}

.user-msg-group { 
	align-items: flex-end; 
}

.chat-bubble-ai, .chat-bubble-user {
	padding: 30rpx;
	max-width: 85%;
	border-radius: 32rpx;
	font-size: 28rpx;
	line-height: 1.6;
}

.chat-bubble-ai {
	background: #fff;
	border-bottom-left-radius: 8rpx;
	color: #1e293b;
	@media (prefers-color-scheme: dark) { background: #1e293b; color: #f1f5f9; }
}

.chat-bubble-user {
	background: $primary;
	border-bottom-right-radius: 8rpx;
	color: #fff;
	box-shadow: 0 10rpx 20rpx rgba(59, 130, 246, 0.3);
}

/* 特殊卡片内容 */
.msg-text-bold { 
	font-weight: 600; 
	margin-bottom: 20rpx; 
	display: block; 
	
	.highlight { 
		color: $primary; 
		font-weight: 800; 
	} 
}

.briefing-box {
	background: #f8fafc;
	padding: 20rpx;
	border-radius: 20rpx;
	margin: 20rpx 0;
	@media (prefers-color-scheme: dark) { background: #0f172a; }
	
	.brief-item {
		display: flex; 
		justify-content: space-between; 
		margin-bottom: 10rpx;
		
		.brief-label { 
			font-size: 22rpx; 
			color: #64748b; 
		}
		
		.brief-val { 
			font-size: 22rpx; 
			font-weight: 700; 
		}
		
		.primary { color: $primary; }
		.danger { color: #ef4444; }
	}
}

.advice { 
	font-size: 24rpx; 
	font-style: italic; 
	color: #64748b; 
	@media (prefers-color-scheme: dark) { color: #94a3b8; } 
}

.msg-img { 
	width: 100%; 
	height: 240rpx; 
	border-radius: 20rpx; 
	margin-bottom: 16rpx; 
}

/* 识别结果卡片 */
.result-header {
	display: flex; 
	flex-wrap: wrap; 
	align-items: center; 
	gap: 12rpx; 
	margin-bottom: 20rpx;
	
	.result-title { 
		font-weight: 700; 
	}
	
	.success { 
		color: #22c55e; 
		font-size: 32rpx; 
	}
	
	.result-tag { 
		background: rgba(59, 130, 246, 0.1); 
		color: $primary; 
		font-size: 20rpx; 
		padding: 4rpx 12rpx; 
		border-radius: 100rpx; 
		font-weight: 700; 
	}
}

.action-btns {
	margin-top: 30rpx;
	display: flex; 
	flex-direction: column; 
	gap: 16rpx;
	
	button { 
		width: 100%; 
		height: 80rpx; 
		line-height: 80rpx; 
		font-size: 26rpx; 
		border-radius: 20rpx; 
		font-weight: 600; 
	}
	
	.primary-btn { 
		background: $primary; 
		color: #fff; 
	}
	
	.outline-btn { 
		background: transparent; 
		border: 1rpx solid #e2e8f0; 
		color: #64748b; 
		@media (prefers-color-scheme: dark) { 
			border-color: #334155; 
			color: #cbd5e1; 
		} 
	}
}

/* 图表样式 */
.chart-header {
	display: flex; 
	justify-content: space-between; 
	margin-bottom: 30rpx;
	
	.chart-title { 
		font-size: 28rpx; 
		font-weight: 700; 
		display: block; 
	}
	
	.chart-unit { 
		font-size: 20rpx; 
		color: #94a3b8; 
	}
	
	.live-tag { 
		font-size: 18rpx; 
		background: rgba(34, 197, 94, 0.1); 
		color: #22c55e; 
		padding: 4rpx 12rpx; 
		border-radius: 8rpx; 
		font-weight: 700;
		height: 36rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}
}

.chart-container-inner {
	height: 120rpx; 
	position: relative;
	
	.chart-svg { 
		width: 100%; 
		height: 100%; 
	}
	
	.chart-popover {
		position: absolute; 
		top: -10rpx; 
		right: 20rpx; 
		background: $primary; 
		color: #fff;
		padding: 8rpx 16rpx; 
		border-radius: 16rpx; 
		display: flex; 
		flex-direction: column;
		
		.pop-val { 
			font-size: 22rpx; 
			font-weight: 700; 
		}
		
		.pop-time { 
			font-size: 16rpx; 
			opacity: 0.8; 
		}
	}
}

.chart-actions {
	display: flex; 
	gap: 16rpx; 
	margin-top: 20rpx;
	
	view { 
		flex: 1; 
		height: 60rpx; 
		line-height: 60rpx; 
		text-align: center; 
		border-radius: 12rpx; 
		font-size: 22rpx; 
		font-weight: 700; 
	}
	
	.chart-btn-gray { 
		background: #f1f5f9; 
		color: #64748b; 
		@media (prefers-color-scheme: dark) { 
			background: #334155; 
			color: #f1f5f9; 
		} 
	}
	
	.chart-btn-primary { 
		background: $primary; 
		color: #fff; 
	}
}

/* Footer 输入框 */
.footer {
	padding: 20rpx 30rpx;
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: blur(20px);
	border-top: 1rpx solid #e2e8f0;
	@media (prefers-color-scheme: dark) { 
		background: rgba(15, 23, 42, 0.9); 
		border-color: #1e293b; 
	}

	.shortcut-scroll {
		white-space: nowrap; 
		margin-bottom: 24rpx;
		
		.shortcut-item {
			display: inline-flex; 
			align-items: center; 
			gap: 8rpx; 
			padding: 12rpx 24rpx;
			background: #fff; 
			border: 1rpx solid #e2e8f0; 
			border-radius: 100rpx; 
			margin-right: 16rpx;
			font-size: 24rpx; 
			font-weight: 600; 
			color: #475569;
			@media (prefers-color-scheme: dark) { 
				background: #1e293b; 
				border-color: #334155; 
				color: #f1f5f9; 
			}
			
			.emoji { 
				font-size: 28rpx; 
			}
		}
	}

	.input-row {
		display: flex; 
		align-items: center; 
		gap: 24rpx; 
		padding-bottom: 20rpx;
		
		.add-icon { 
			color: #94a3b8; 
			font-size: 48rpx; 
		}
		
		.input-box {
			flex: 1; 
			background: #f1f5f9; 
			border-radius: 100rpx; 
			padding: 0 30rpx;
			display: flex; 
			align-items: center; 
			height: 84rpx;
			@media (prefers-color-scheme: dark) { background: #334155; }
			
			.real-input { 
				flex: 1; 
				font-size: 28rpx; 
				color: #1e293b; 
				@media (prefers-color-scheme: dark) { color: #fff; } 
			}
			
			.mic-icon { 
				color: #94a3b8; 
				font-size: 40rpx; 
			}
		}
		
		.send-btn {
			width: 84rpx; 
			height: 84rpx; 
			background: $primary; 
			border-radius: 50%;
			display: flex; 
			align-items: center; 
			justify-content: center; 
			color: #fff;
			box-shadow: 0 8rpx 20rpx rgba(59, 130, 246, 0.3);
		}
	}
}

.home-bar { 
	width: 240rpx; 
	height: 8rpx; 
	background: #e2e8f0; 
	border-radius: 100rpx; 
	margin: 10rpx auto 0; 
	@media (prefers-color-scheme: dark) { background: #334155; } 
}

.card-shadow { 
	box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.03); 
}

.loading-dots {
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 10rpx 0;
	
	.dot-loading {
		width: 16rpx;
		height: 16rpx;
		background: #94a3b8;
		border-radius: 50%;
		animation: bounce 1.4s infinite ease-in-out both;
		
		&:nth-child(1) { 
			animation-delay: -0.32s; 
		}
		
		&:nth-child(2) { 
			animation-delay: -0.16s; 
		}
	}
}

@keyframes bounce {
	0%, 80%, 100% { 
		transform: scale(0); 
	}
	40% { 
		transform: scale(1); 
	}
}
</style>
