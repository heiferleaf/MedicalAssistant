<template>
	<view class="chat-container">
		<view class="status-bar"></view>
		
		<!-- 背景层：仅在没有任何消息时显示 -->
		<view class="chat-background" v-if="messages.length === 0">
			<image class="bg-logo" src="/static/ai/bg_logo.png" mode="aspectFit" />
			<text class="bg-text">咨询健康问题，获取专业建议</text>
		</view>
		
		<!-- 会话侧边栏 -->
		<SessionSidebar
			:visible="showSidebar"
			:sessions="sessions"
			:currentSessionId="currentSessionId"
			@new="createNewSession"
			@close="toggleSidebar"
			@switch="switchSession"
			@delete="deleteSession"
			@update-name="handleUpdateSessionName"
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
				@load-more="loadMoreMessages"
				@scroll-to="scrollToMessage"
				@action-confirm="handleActionConfirm"
				@action-edit="handleActionEdit"
				@action-cancel="handleActionCancel"
			/>
			
			<!-- 底部输入区 -->
			<view class="footer">
				<!-- 图片预览区域 -->
				<view v-if="showImagePreview && scanImage" class="image-preview-bar">
					<!-- H5 环境使用 img 标签 -->
					<img v-if="isH5" :src="scanImageBase64 ? `data:image/jpeg;base64,${scanImageBase64}` : scanImage" class="preview-image"/>
					<image v-else :src="scanImage" mode="aspectFill" class="preview-image"/>
					<view class="remove-btn" @click="removeImage">
						<image src="/static/Register/close.png" class="remove-icon"/>
					</view>
				</view>
				
				<!-- 输入框 -->
				<ChatInput
					@send="sendMessage"
					@camera="handleCamera"
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
import SessionSidebar from './components/SessionSidebar.vue';

// 导入工具函数
import { 
	createMessage, 
	createMessageWithAction,
	StorageKeys,
	getFromStorage,
	setToStorage,
	generateSessionId 
} from './utils/chatUtils';

// 导入 API
import agentApi from '@/api/agent';
import sessionApi from '@/api/session';
import reminderApi from '@/api/reminder';
import medicineApi from '@/api/medicine';

export default {
	name: 'Assistant',
	components: {
		ChatHeader,
		ChatView,
		ChatInput,
		SessionSidebar
	},
	data() {
		return {
			messages: [],
			sessions: [],
			currentSessionId: '',
			sessionId: '',
			userId: '',
			showSidebar: false,
			scrollToMsgId: '',
			loading: false,
			// 新增：拍照传来的图片
			scanImage: '',
			// 新增：显示图片预览区域
			showImagePreview: false,
			// 图片 Base64 数据（用于发送）
			scanImageBase64: ''
		}
	},
	computed: {
		isH5() {
			// #ifdef H5
			return true;
			// #endif
			return false;
		}
	},
	onLoad(options) {
		// 检查是否从拍照识别跳转过来
		if (options.from === 'scan') {
			// 从缓存中读取图片
			const imageData = uni.getStorageSync('last_scan_image');
			if (imageData) {
				this.scanImage = imageData;
				this.showImagePreview = true;
				
				// #ifdef H5
				// H5 端保存的是路径，需要从 localStorage 读取 Base64
				if (imageData.includes('/images/drug_')) {
					const fileName = imageData.split('/').pop();
					const base64 = uni.getStorageSync('drug_image_' + fileName);
					if (base64) {
						this.scanImageBase64 = base64;
						console.log('从本地加载图片 Base64，长度:', base64.length);
					} else {
						console.error('未找到图片数据:', fileName);
					}
				} else {
					// 兼容旧逻辑（直接存 Base64）
					this.scanImageBase64 = imageData;
					console.log('接收到 Base64 图片，长度:', imageData.length);
				}
				// #endif
				
				// #ifdef APP-PLUS
				// App 端保存的是路径
				console.log('接收到拍照图片:', imageData);
				// #endif
			}
		}
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
						createdAt: new Date().toISOString(),
						needsNaming: true  // 标记需要命名
					};
					
					this.sessions.unshift(newSession);
					this.switchSession(newSession);
					this.showSidebar = false;
					
					// 清除待确认请求（避免影响新会话）
					try {
						// 调用后端接口删除所有待确认请求
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
		
		// 切换会话
		async switchSession(session) {
			this.currentSessionId = session.sessionId;
			this.sessionId = session.sessionId;
			
			// 清除待确认请求（避免影响新会话）
			try {
				// 调用后端接口删除所有待确认请求
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
				try {
					const res = await sessionApi.getMessages(this.sessionId);
					
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
															
									return createMessageWithAction(
										msg.role,
										msg.content,
										msg.actionType,
										wrappedActionData
									);
								} catch (e) {
									console.error('解析 actionData 失败:', e);
									return createMessage(msg.role, msg.content);
								}
							} else {
								return createMessage(msg.role, msg.content, 'text', {
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
		
		// 更新会话名称
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
		},
		
		// 发送消息
		async sendMessage(content) {
			// 检查是否有图片
			const hasImage = this.showImagePreview && this.scanImage;
			
			// 空值检查（如果没有图片，则需要文字内容）
			if (!hasImage && (!content || !content.trim())) {
				console.warn('sendMessage: 消息内容为空，已忽略');
				return;
			}
			
			// 检查是否需要为新会话命名（用户的第一句话）
			const currentSession = this.sessions.find(s => s.sessionId === this.sessionId);
			if (currentSession && currentSession.needsNaming) {
				// 直接使用用户第一句话作为会话名称（截取前 20 个字）
				currentSession.summary = content ? content.substring(0, 20) : '图片识别';
				currentSession.needsNaming = false;
			}
			
			// 添加用户消息
			let userMsg;
			if (hasImage) {
				// 如果有图片，创建图片消息
				// 确保 imagePath 是完整的 data URL，这样保存到本地存储后才能直接显示
				console.log('创建图片消息，scanImageBase64 长度:', this.scanImageBase64 ? this.scanImageBase64.length : 0);
				const fullImageDataUrl = this.scanImageBase64 ? `data:image/jpeg;base64,${this.scanImageBase64}` : this.scanImage;
				console.log('imagePath 设置:', fullImageDataUrl.substring(0, 50) + '...');
				
				userMsg = {
					id: Date.now().toString(),
					role: 'user',
					type: 'image',
					content: content || '帮我识别这个药品',
					imagePath: fullImageDataUrl,
					createdAt: new Date().toISOString()
				};
			} else {
				// 纯文字消息
				userMsg = createMessage('user', content);
			}
			this.messages.push(userMsg);
			this.scrollToBottom();
			
			// 添加加载状态
			const loadingMsg = createMessage('loading', '');
			this.messages.push(loadingMsg);
			this.scrollToBottom();
			this.loading = true;
			
			// 模拟状态变化（实际应该根据后端返回的工具调用状态来切换）
			const statusTimer = setTimeout(() => {
			}, 2000);
			
			try {
				// 准备发送给 AI 的消息
				let messageToSend = content || '帮我识别这个药品';
				
				// 如果有图片，需要转换为 Base64 并发送给 AI
				if (hasImage) {
					console.log('开始转换图片为 Base64...');
					console.log('scanImage 类型:', typeof this.scanImage);
					console.log('scanImage 长度:', this.scanImage ? this.scanImage.length : 'null');
					console.log('scanImage 前 50 字符:', this.scanImage ? this.scanImage.substring(0, 50) : 'null');
					
					const base64 = await this.imageToBase64(this.scanImage);
					console.log('图片转换成功，长度:', base64.length);
					console.log('base64 前 50 字符:', base64.substring(0, 50));
					
					// 构造特殊的消息格式，告诉 AI 调用 OCR 工具
					messageToSend = `用户发送了一张药物图片，请使用 recognizeDrugFromImage 工具识别图片中的药物信息。图片数据：${base64}`;
				}
				
				console.log('发送消息给 AI:', messageToSend.substring(0, 100) + '...');
				console.log('请求参数:', {
					user_id: this.userId,
					session_id: this.sessionId,
					message_length: messageToSend.length
				});
				
				// 使用流式输出
				let assistantMessage = '';
				let actionType = null;
				let actionData = null;
				
				// 创建一个空的消息占位符（不显示，等有了内容再添加）
				let assistantMsg = null;
				
				// 工具执行步骤
				let toolSteps = [];
				
				await agentApi.chatStream({
					user_id: this.userId,
					session_id: this.sessionId,
					message: messageToSend,
					onChunk: (chunk) => {
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
								assistantMsg = createMessage('assistant', '');
								assistantMsg.toolSteps = toolSteps;
								this.messages.push(assistantMsg);
								this.scrollToBottom();
							} else {
								// 更新工具步骤
								assistantMsg.toolSteps = toolSteps;
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
									assistantMsg.toolSteps = [...toolSteps];
								}
							}
						}
					}
				});
				
				console.log('AI 返回:', { assistantMessage, actionType, actionData });
				
				// 清除状态切换定时器
				clearTimeout(statusTimer);
				
				// 移除加载状态（通过查找 role='loading'的消息）
				const loadingIndex = this.messages.findIndex(m => m.role === 'loading');
				if (loadingIndex !== -1) {
					this.messages.splice(loadingIndex, 1);
				}
				this.loading = false;
				
				// 移除图片预览（在发送成功后）
				if (hasImage) {
					this.removeImage();
				}
				
				// 如果有 action 数据，需要更新消息
				if (actionType && actionData) {
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
				} else {
					// 检查 tool 返回结果中的 pending_confirmation 标记
					// 注意：流式模式下，这些数据应该在 actionData 中处理
					
					// 检测特殊标记：[ACTION:plan_confirm], [ACTION:plan_update], [ACTION:plan_delete], [ACTION:addMedicine], [ACTION:updateTaskStatus]
					// 注意：需要捕获 [ACTION:xxx] 之前的内容
					const actionMatch = assistantMessage?.match(/([\s\S]*?)\[ACTION:(plan_\w+|addMedicine|updateTaskStatus)\]/);
					
					if (actionMatch) {
						const actionType = actionMatch[2]; // plan_confirm, plan_update, plan_delete, addMedicine, updateTaskStatus
						const actionData = actionMatch[1].trim(); // [ACTION:xxx] 之前的内容
						
						console.log('检测到特殊标记:', actionType, actionData);
						
						if (actionType === 'addMedicine') {
							// 显示药箱确认卡片
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
						const contentSafe = content || '';
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
									medicineName: this.extractMedicineName(content) || '',
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
				
				this.scrollToBottom();
				
				// 保存到本地存储
				this.saveMessagesToStorage();
				
				// 更新会话预览（如果不是新会话命名，则正常更新）
				const currentSession = this.sessions.find(s => s.sessionId === this.sessionId);
				if (!currentSession || !currentSession.needsNaming) {
					this.updateSessionPreview(content, assistantMessage);
				}
			} catch (error) {
				console.error('请求失败:', error);
				clearTimeout(statusTimer);
				// 移除加载状态（通过查找 role='loading'的消息）
				const loadingIndex = this.messages.findIndex(m => m.role === 'loading');
				if (loadingIndex !== -1) {
					this.messages.splice(loadingIndex, 1);
				}
				this.loading = false;
				const errorMsg = createMessage('assistant', '抱歉，网络开小差了，请稍后再试。');
				this.messages.push(errorMsg);
				this.scrollToBottom();
			} finally {
				this.loading = false;
				
				// 检查是否有待确认的请求（Human-in-the-loop）
				this.checkPendingRequests();
			}
		},
		
		// 检查待确认的 Tool 请求
		async checkPendingRequests() {
			try {
				const response = await agentApi.getPendingRequests(this.userId);
				console.log('待确认请求 response:', response);
				
				// uni.request 返回的格式是 {data, statusCode, ...}
				// 真正的数据在 response.data 中
				const responseData = response.data || response;
				console.log('responseData:', responseData);
				
				// 兼容不同的响应格式
				let dataList = [];
				
				// 格式 1: { code: 200, data: [...] }
				if (responseData.code === 200 && Array.isArray(responseData.data)) {
					dataList = responseData.data;
				}
				// 格式 2: { success: true, data: [...] }
				else if (responseData.success && Array.isArray(responseData.data)) {
					dataList = responseData.data;
				}
				// 格式 3: 直接是数组
				else if (Array.isArray(responseData)) {
					dataList = responseData;
				}
				
				console.log('dataList:', dataList);
				
				if (dataList.length > 0) {
					// 有待确认的请求
					const pending = dataList[0];
					console.log('检测到待确认请求:', pending);
					console.log('toolName:', pending.toolName);
					console.log('toolArguments:', pending.toolArguments);
					console.log('toolArguments 类型:', typeof pending.toolArguments);
					
					// 根据 tool 类型显示不同的卡片
					if (pending.toolName === 'createPlan') {
						// 解析参数（兼容字符串和对象）
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
						console.log('planData:', planData);
						
						// 显示确认卡片
						const assistantMsg = createMessageWithAction(
							'assistant',
							'请确认是否创建以下用药计划：',
							'plan',
							{
								data: planData,
								showConfirm: true,
								showEdit: true,
								requestId: pending.requestId  // 重要！用于后续执行
							}
						);
						this.messages.push(assistantMsg);
						this.scrollToBottom();
					} else if (pending.toolName === 'addMedicine') {
						// 解析参数（兼容字符串和对象）
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
						console.log('medicineData:', medicineData);
						
						// 显示确认卡片
						const assistantMsg = createMessageWithAction(
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
								requestId: pending.requestId  // 重要！用于后续执行
							}
						);
						this.messages.push(assistantMsg);
						this.scrollToBottom();
					}
					// TODO: 其他 tool 类型的处理
				} else {
					console.log('没有待确认的请求');
				}
			} catch (error) {
				console.error('检查待确认请求失败:', error);
				console.error('错误堆栈:', error.stack);
				// 不显示错误，避免影响用户体验
			}
		},
		
		// 解析特殊标记中的数据
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
		
		// 从用户消息中提取药品名称
		extractMedicineName(content) {
			// 尝试从"创建用药计划 xxx"中提取药品名
			const match = content.match(/创建 (?:用药)? 计划 [：:]\s*(.+)/);
			if (match && match[1]) {
				return match[1].trim();
			}
			// 或者从"我想创建 xxx 的用药计划"中提取
			const match2 = content.match(/创建\s+(.+?)\s+(?:的)? 用药计划/);
			if (match2 && match2[1]) {
				return match2[1].trim();
			}
			// 从"添加 xxx 到药箱"中提取
			const match3 = content.match(/添加 (.+?) 到药箱/);
			if (match3 && match3[1]) {
				return match3[1].trim();
			}
			// 从"吃了 xxx"、"服用了 xxx"中提取
			const match4 = content.match(/(?:吃了 | 服用了 | 吃了)(.+?)(?:药|片|胶囊|)/);
			if (match4 && match4[1]) {
				return match4[1].trim();
			}
			return '';
		},
		
		extractDosage(content) {
			// 尝试从"剂量 xxx"、"每次 xxx"中提取
			const match = content.match(/(?:剂量 | 每次 | 用量)[：:]\s*(.+?)(?:，|,|。|$)/);
			if (match && match[1]) {
				return match[1].trim();
			}
			// 从"xxx 片"、"xxx mg"中提取
			const match2 = content.match(/(\d+\s*(?:片|粒|mg|ml|克))/);
			if (match2 && match2[1]) {
				return match2[1].trim();
			}
			return '';
		},
		
		extractTimePoint(content) {
			// 尝试从"时间 xxx"、"xxx 点"中提取
			const match = content.match(/(?:时间 | 在|点)[：:]\s*(\d{1,2}[：:]\d{2})/);
			if (match && match[1]) {
				return match[1].trim();
			}
			// 从"早上 xxx"、"上午 xxx"、"下午 xxx"、"晚上 xxx"中提取
			const match2 = content.match(/(早上 | 上午 | 下午 | 晚上)\s*(\d{1,2}(?:[：:]\d{2})?)/);
			if (match2 && match2[2]) {
				return match2[2].trim();
			}
			// 从"xxx:xx"格式中提取
			const match3 = content.match(/(\d{1,2}:\d{2})/);
			if (match3 && match3[1]) {
				return match3[1].trim();
			}
			return '';
		},
		
		// 处理相机按钮（拍照识别）
		handleCamera() {
			console.log('点击相机按钮，跳转拍照识别');
			uni.navigateTo({
				url: '/pages/scan/DrugScan?from=chat',
				animationType: 'fade-in',
				animationDuration: 300
			});
		},
		
		// 移除图片
		removeImage() {
			// #ifdef H5
			// H5 端：清理缓存的图片数据
			if (this.scanImage && this.scanImage.includes('/images/drug_')) {
				const fileName = this.scanImage.split('/').pop();
				uni.removeStorageSync('drug_image_' + fileName);
				console.log('清理图片缓存:', fileName);
			}
			// #endif
			
			this.scanImage = '';
			this.scanImageBase64 = '';
			this.showImagePreview = false;
			uni.removeStorageSync('last_scan_image');
		},
		
		// 图片转 Base64
		imageToBase64(imagePath) {
			return new Promise((resolve, reject) => {
				// #ifdef APP-PLUS
				// App 端：路径需要转换
				plus.io.resolveLocalFileSystemURL(imagePath, (entry) => {
					entry.file((file) => {
						const reader = new plus.io.FileReader();
						reader.onload = (e) => {
							const base64 = e.target.result.split(',')[1]; // 去掉 data:image/jpeg;base64, 前缀
							resolve(base64);
						};
						reader.onerror = reject;
						reader.readAsDataURL(file);
					});
				}, reject);
				// #endif
				
				// #ifdef H5
				// H5 端：直接使用已加载的 Base64 数据
				console.log('H5 端直接使用 Base64 数据，长度:', this.scanImageBase64 ? this.scanImageBase64.length : 0);
				resolve(this.scanImageBase64 || '');
				// #endif
			});
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
		
		// 处理操作卡片确认
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
		
		// 处理操作卡片取消
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
		
		// 处理操作卡片编辑
		handleActionEdit({ type, data }) {
			console.log('操作编辑:', type, data);
			// 卡片内部会处理编辑逻辑，这里不需要额外操作
		},
		
		// 创建用药计划（用户确认后调用后端真正创建）
		async handleCreatePlanWithApproval(data, messageId) {
			console.log('handleCreatePlanWithApproval 被调用, messageId:', messageId);
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
		
		// 更新用药任务状态（用户确认后调用后端真正更新）
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
		
		// 添加药品到药箱（用户确认后调用后端真正添加）
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
		
		// 更新卡片状态
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
		},
		
		// 保存消息到本地存储
		saveMessagesToStorage() {
			try {
				const storageKey = `${StorageKeys.MESSAGES}_${this.sessionId}`;
				setToStorage(storageKey, this.messages);
				console.log('消息已保存到本地存储');
			} catch (error) {
				console.error('保存消息失败:', error);
			}
		},
		
		// 创建用药计划（传统模式）
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
		}
	}
}
</script>

<style lang="scss" scoped>
.chat-container {
	height: 100vh;
	display: flex;
	flex-direction: row;
	background-color: #f8fafc;
	overflow: hidden;
	position: relative;
	@media (prefers-color-scheme: dark) { background-color: #0f172a; }
}

// 背景层样式
.chat-background {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	z-index: 0;
	pointer-events: none; // 让背景层不阻挡点击事件
	
	.bg-logo {
		width: 400rpx;
		height: 400rpx;
		opacity: 1; // 正常显示，不调暗
		margin-bottom: 40rpx;
	}
	
	.bg-text {
		font-size: 32rpx;
		color: #6366f1; // 使用主题紫色，正常亮度
		text-align: center;
		opacity: 1; // 正常显示
		max-width: 80%;
		font-weight: 500;
		@media (prefers-color-scheme: dark) {
			color: #818cf8; // 深色模式下稍亮
		}
	}
}

.status-bar { 
	height: var(--status-bar-height); 
	background: #ffffff; 
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	z-index: 1000;
	@media (prefers-color-scheme: dark) { background: #0f172a; } 
}

.main-content {
	flex: 1;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.footer {
	position: relative;
	z-index: 100;
	background: transparent;  // 移除背景，让输入框悬浮
	border-top: none;  // 移除顶部边框
	
	@media (prefers-color-scheme: dark) { 
		background: transparent;
	}
	
	// 图片预览条
	.image-preview-bar {
		display: flex;
		align-items: center;
		background: #f1f5f9;
		border-radius: 24rpx;
		padding: 12rpx;
		margin: 20rpx 30rpx 0;
		position: relative;
		@media (prefers-color-scheme: dark) {
			background: #334155;
		}
		
		.preview-image {
			width: 120rpx;
			height: 120rpx;
			border-radius: 8rpx;
			object-fit: cover;
		}
		
		.remove-btn {
			position: absolute;
			top: -16rpx;
			right: -16rpx;
			width: 48rpx;
			height: 48rpx;
			background: #ef4444;
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			box-shadow: 0 2rpx 8rpx rgba(239, 68, 68, 0.3);
			
			.remove-icon {
				width: 28rpx;
				height: 28rpx;
			}
		}
	}
}
</style>
