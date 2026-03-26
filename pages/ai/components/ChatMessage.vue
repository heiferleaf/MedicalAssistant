<template>
	<view :class="['message-group', role === 'user' ? 'user-message' : 'ai-message']">
		<!-- AI 消息：完整的圆角矩形，占满整个空间 -->
		<view v-if="role === 'assistant'" class="ai-message-container">
			<view class="ai-content">
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
				
				<!-- AI 消息使用 Markdown 渲染 -->
				<SimpleMarkdown 
					v-if="type === 'text' && !actionType" 
					:content="content" 
				/>
				
				<!-- 图片消息：H5 使用 img 标签，其他平台使用 image 标签 -->
				<view v-if="image && isH5">
					<img 
						:src="image" 
						class="msg-img"
						@error="handleImageError"
						@load="handleImageLoad"
					/>
					<!-- 调试：显示 Base64 前缀 -->
					<view style="font-size: 10px; color: #999; margin-top: 4px;">
						Base64 前缀：{{ image.substring(0, 30) }}...
					</view>
				</view>
				<image 
					v-if="!isH5 && image" 
					class="msg-img" 
					:src="image" 
					mode="aspectFill"
				/>
				
				<!-- 加载状态 -->
				<view v-if="role === 'loading'" class="loading-dots">
					<view class="dot-loading"></view>
					<view class="dot-loading"></view>
					<view class="dot-loading"></view>
				</view>
				
				<!-- 插槽：用于扩展其他消息类型 -->
				<slot></slot>
			</view>
		</view>
		
		<!-- 用户消息：保持原有样式 -->
		<view v-else :class="['chat-bubble', 'chat-bubble-user']">
			<text v-if="type === 'text'" class="msg-text">{{ content }}</text>
			<!-- 图片消息：H5 使用 img 标签，其他平台使用 image 标签 -->
			<img 
				v-if="isH5 && image" 
				:src="image" 
				class="msg-img"
				@error="handleImageError"
				@load="handleImageLoad"
			/>
			<image 
				v-if="!isH5 && image" 
				class="msg-img" 
				:src="image" 
				mode="aspectFill"
			/>
			<slot></slot>
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
			type: String,
			default: ''
		},
		image: {
			type: String,
			default: ''
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
			isH5: uni.getSystemInfoSync().platform === 'web' || window !== undefined
		};
	},
	watch: {
			image: {
				immediate: true,
				handler(newVal) {
					if (newVal) {
						console.log('ChatMessage 收到图片:', newVal.substring(0, 50) + '...');
					} else {
						console.log('ChatMessage image 为空');
					}
				}
			}
		},
	methods: {
		// 处理图片加载成功
		handleImageLoad(e) {
			console.log('图片加载成功:', e);
		},
		
		// 处理图片加载失败
		handleImageError(e) {
			console.error('图片加载失败:', e);
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
$primary: #3B82F6;

.message-group {
	margin-bottom: 40rpx;
	display: flex;
	flex-direction: column;
	
	&.user-message {
		align-items: flex-end;
	}
	
	&.ai-message {
		align-items: stretch;
	}
}

/* AI 消息样式 - 完整圆角矩形，占满整个空间 */
.ai-message-container {
	width: 100%;
	background: #fff;
	border-radius: 24rpx;
	overflow: hidden;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
	
	@media (prefers-color-scheme: dark) {
		background: #1e293b;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.3);
	}
}

.ai-content {
	padding: 32rpx;
	font-size: 28rpx;
	line-height: 1.8;
	color: #1e293b;
	
	@media (prefers-color-scheme: dark) {
		color: #f1f5f9;
	}
}

/* 用户消息样式 - 保持原有 */
.chat-bubble {
	padding: 30rpx;
	max-width: 85%;
	border-radius: 32rpx;
	font-size: 28rpx;
	line-height: 1.6;
	
	&.chat-bubble-user {
		background: $primary;
		border-bottom-right-radius: 8rpx;
		color: #fff;
		box-shadow: 0 10rpx 20rpx rgba(59, 130, 246, 0.3);
	}
}

.msg-text {
	display: block;
}

.msg-img {
	width: 100%;
	height: 240rpx;
	border-radius: 20rpx;
	margin-bottom: 16rpx;
	display: block;
	object-fit: cover;
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
