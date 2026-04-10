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
					<!-- 使用 image 标签，使用 Base64 显示 -->
					<image :src="scanImageBase64 || scanImage" mode="aspectFill" class="preview-image"/>
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
		
		<!-- #ifdef APP-PLUS -->
		<!-- App 端专用的 SSE 组件（隐藏） -->
		<ChatSSEClient ref="sseClient" style="display: none;"/>
		<!-- OCR 上传组件（隐藏） -->
		<OCRUploader ref="ocrUploader" style="display: none;" @ocr-success="handleOCRSuccess" @ocr-error="handleOCRError"/>
		<!-- #endif -->
	</view>
</template>

<script>
// 导入组件
import ChatHeader from './components/ChatHeader.vue';
import ChatView from './views/ChatView.vue';
import ChatInput from './components/ChatInput.vue';
import SessionSidebar from './components/SessionSidebar.vue';
// #ifdef APP-PLUS
import ChatSSEClient from '@/components/ChatSSEClient/ChatSSEClient.vue'
import OCRUploader from '@/components/OCRUploader/OCRUploader.vue'
// #endif

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

// 导入方法模块
import sessionMethods from './methods/sessionMethods';
import imageMethods from './methods/imageMethods';
import messageMethods from './methods/messageMethods';
import chatMethods from './methods/chatMethods';
import chatStreamMethods from './methods/chatStreamMethods';
import chatActionMethods from './methods/chatActionMethods';

export default {
	name: 'Assistant',
	
	// 混入方法模块
	mixins: [sessionMethods, imageMethods, messageMethods, chatMethods, chatStreamMethods, chatActionMethods],
	
	components: {
		ChatHeader,
		ChatView,
		ChatInput,
		SessionSidebar
		// #ifdef APP-PLUS
		,ChatSSEClient, OCRUploader
		// #endif
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
			scanImageBase64: '',
			// #ifdef APP-PLUS
			// OCR 相关数据
			ocrData: null,
			ocrLoading: false,
			ocrResult: '',
			// #endif
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
				// App 端保存的是路径，需要转换为 Base64
				console.log('接收到拍照图片:', imageData);
				// 使用 Promise 包装异步转换
				this.convertImagePathToBase64(imageData).then(base64 => {
					this.scanImageBase64 = base64;
					console.log('图片路径转换为 Base64，长度:', base64.length);
				}).catch(err => {
					console.error('转换图片失败:', err);
				});
				// #endif
			}
		}
		this.init();
	},
	// #ifdef APP-PLUS
	mounted() {
		// 等待 DOM 更新后再注册
		this.$nextTick(() => {
			// 注册全局 SSE 桥接对象
			uni.$sseBridge = {
				send: (config) => {
					if (this.$refs.sseClient) {
						this.$refs.sseClient.startChat(config)
					} else {
						console.error('SSE 组件未初始化，$refs.sseClient:', this.$refs.sseClient)
					}
				},
				stop: () => {
					if (this.$refs.sseClient) {
						this.$refs.sseClient.stopChat()
					}
				}
			}
			console.log('Assistant: SSE 桥接已注册，$refs:', Object.keys(this.$refs || {}))
		})
	},
	// #endif
	
	// 拦截返回按钮，强制返回到主页
	onBackPress() {
		console.log('onBackPress 被调用，准备返回主页');
		uni.reLaunch({
			url: '/pages/index/index',
			success: () => {
				console.log('reLaunch 成功');
			},
			fail: (err) => {
				console.error('reLaunch 失败:', err);
				// 如果 reLaunch 失败，使用 redirectTo
				uni.redirectTo({
					url: '/pages/index/index'
				});
			}
		});
		return true; // 阻止默认返回行为
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
