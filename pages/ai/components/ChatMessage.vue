<template>
	<view :class="['message-group', role === 'user' ? 'user-message' : 'ai-message']">
		<!-- 加载状态：直接显示 loading 动画，不显示气泡 -->
		<view v-if="role === 'loading'" class="loading-dots">
			<view class="dot-loading"></view>
			<view class="dot-loading"></view>
			<view class="dot-loading"></view>
		</view>
		
		<!-- AI 消息：完整的圆角矩形，占满整个空间 -->
		<view v-else-if="role === 'assistant'" class="ai-message-container">
			<view class="ai-content">
				<!-- 工具执行步骤 -->
				<!-- 工具执行步骤 -->
				<ToolSteps v-if="toolSteps && toolSteps.length > 0" :steps="toolSteps" />
					
				<!-- 操作卡片：用药计划 -->
				<PlanActionCard
					v-if="actionType === 'plan'"
					:messageId="messageId"
					:actionData="actionData.data"
					:showConfirm="actionData.showConfirm"
					:showEdit="actionData.showEdit"
					:status="actionData.status"
					@confirm="handleActionConfirm"
					@cancel="handleActionCancel"
				/>
				
				<!-- 操作卡片：用药任务 -->
				<TaskActionCard
					v-if="actionType === 'task'"
					:messageId="messageId"
					:actionData="actionData.data"
					:status="actionData.status"
					@confirm="handleActionConfirm"
					@cancel="handleActionCancel"
				/>
				
				<!-- 操作卡片：药箱 -->
				<MedicineActionCard
					v-if="actionType === 'medicine'"
					:messageId="messageId"
					:actionData="actionData.data"
					:status="actionData.status"
					@confirm="handleActionConfirm"
					@cancel="handleActionCancel"
				/>
				
				<!-- 图片显示在文字上方 -->
				<image 
					v-if="shouldShowImage" 
					class="msg-img" 
					:src="image" 
					mode="aspectFill"
					@load="handleImageLoad"
					@error="handleImageError"
				/>
				
				<!-- AI 消息使用 Markdown 渲染 -->
				<SimpleMarkdown 
					v-if="type === 'text' && content && content.trim() !== ''" 
					:content="content" 
				/>
				
				<!-- 插槽：用于扩展其他消息类型 -->
				<slot></slot>
			</view>
		</view>
		
		<!-- 用户消息容器（文字 + 图片） -->
		<view v-if="role === 'user'" class="chat-bubble chat-bubble-user">
			<!-- 图片（如果有） -->
			<image 
				v-if="image" 
				class="msg-img" 
				:src="image" 
				mode="aspectFit"
				show-menu-by-longpress
				@load="handleImageLoad"
				@error="handleImageError"
			/>
			<!-- 文字内容（如果有） -->
			<text v-if="content" class="msg-text">{{ content }}</text>
			<slot name="text"></slot>
		</view>
	</view>
</template>

<script>
import SimpleMarkdown from './SimpleMarkdown.vue';
import PlanActionCard from './ActionCards/PlanActionCard.vue';
import TaskActionCard from './ActionCards/TaskActionCard.vue';
import MedicineActionCard from './ActionCards/MedicineActionCard.vue';
import ToolSteps from './ToolSteps.vue';

export default {
	name: 'ChatMessage',
	components: {
		SimpleMarkdown,
		PlanActionCard,
		TaskActionCard,
		MedicineActionCard,
		ToolSteps
	},
	props: {
		// 消息 ID
		messageId: {
			type: String,
			default: ''
		},
		role: {
			type: String,
			required: true,
			validator: (value) => ['user', 'assistant', 'loading'].includes(value)
		},
		type: {
			type: String,
			default: 'text'
		},
		content: {
			type: [String, Number],
			default: null
		},
		image: {
			type: String,
			default: null
		},
		// 操作类型：plan, medicine, task, family 等
		actionType: {
			type: String,
			default: ''
		},
		// 操作数据
		actionData: {
			type: Object,
			default: () => ({})
		},
		// 工具执行步骤
		toolSteps: {
			type: Array,
			default: () => []
		}
	},
	data() {
		return {
		};
	},
	methods: {
		// 处理图片加载成功
		handleImageLoad(e) {
			console.log('✅ 图片加载成功:', e.detail);
			console.log('图片尺寸:', e.detail.width, 'x', e.detail.height);
		},
				
		// 处理图片加载失败
		handleImageError(e) {
			console.error('❌ 图片加载失败:', e);
			console.error('图片数据:', this.image ? this.image.substring(0, 100) + '...' : '无图片数据');
		},
		
		// 处理确认操作
		handleActionConfirm(data) {
			this.$emit('action-confirm', {
				type: this.actionType,
				data: data,
				messageId: this.messageId
			});
		},
		
		// 处理取消操作
		handleActionCancel(data) {
			this.$emit('action-cancel', {
				type: this.actionType,
				data: data,
				messageId: this.messageId
			});
		},
		
		// 处理编辑操作
		handleActionEdit(data) {
			this.$emit('action-edit', {
				type: this.actionType,
				data: data
			});
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #6366f1;

.message-group {
	margin-bottom: 40rpx;
	display: flex;
	flex-direction: column;
	
	&.user-message {
		align-items: flex-end;  // 用户消息靠右对齐
		width: 100%;  // 确保外层容器占满宽度
	}
	
	&.ai-message {
		align-items: stretch;
	}
}

/* AI 消息样式 - 透明背景，文字直接显示 */
.ai-message-container {
	width: 100%;
	background: transparent;  // 透明背景
	border: none;  // 移除边框
	box-shadow: none;  // 移除阴影
}

.ai-content {
	padding: 16rpx 0;  // 减少内边距，让文字更贴近
	font-size: 28rpx;
	line-height: 1.8;
	color: #1e293b;
	
	@media (prefers-color-scheme: dark) {
		color: #f1f5f9;
	}
}

/* 用户消息样式 - 从右开始显示，不满一行时右对齐 */
.chat-bubble {
	padding: 30rpx;
	max-width: 80%;  // 限制最大宽度为 80%，让消息从右开始
	border-radius: 24rpx;  // 四个角都是大圆角
	font-size: 28rpx;
	line-height: 1.6;
	word-wrap: break-word;  // 允许长单词换行
	word-break: break-word;  // 允许在任意字符间断行
	display: inline-block;  // 根据内容自适应宽度
	
	&.chat-bubble-user {
		background: linear-gradient(135deg, #6366f1, #8b5cf6);
		border-radius: 24rpx;  // 四个角都是大圆角
		color: #fff;
		box-shadow: 0 8rpx 16rpx rgba(99, 102, 241, 0.25);
		margin-left: auto;  // 让消息靠右
	}
}

/* 图片消息框：完全透明，无背景无边框，独立显示 */
.image-message-container {
	width: 100%;
	display: flex;
	justify-content: flex-end;  // 图片靠右对齐，与文字消息一致
	margin-top: 16rpx;  // 与文字消息的间距
	padding: 0 30rpx;  // 与文字消息的内边距一致
	
	.msg-img {
		max-width: 600rpx;  // 限制图片最大宽度
		max-height: 800rpx;  // 限制图片最大高度
		height: auto;  // 高度自适应
		width: auto;  // 宽度自适应
		border-radius: 16rpx;  // 图片圆角
		display: block;
		object-fit: contain;  // 保持图片比例完整显示
		background: transparent;  // 透明背景
	}
}

.msg-text {
	display: block;
	word-wrap: break-word;
	word-break: break-all;
}

/* 调试信息样式 */
.debug-info {
	font-size: 20rpx;
	color: #999;
	margin-top: 10rpx;
	display: block;
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
		
		&:nth-child(1) { animation-delay: -0.32s; }
		&:nth-child(2) { animation-delay: -0.16s; }
	}
}

@keyframes bounce {
	0%, 80%, 100% { transform: scale(0); }
	40% { transform: scale(1); }
}
</style>
