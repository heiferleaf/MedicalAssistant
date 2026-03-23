<template>
	<view class="chat-view">
		<scroll-view 
			scroll-y 
			class="chat-main" 
			:scroll-into-view="scrollToMsgId" 
			scroll-with-animation
			@scrolltolower="loadMore"
		>
			<view class="chat-content">
				<view
					v-for="(msg, index) in messages"
					:key="index"
					:id="'msg-' + index"
				>
					<ChatMessage
						:messageId="msg.id"
						:role="msg.role"
						:type="msg.type"
						:content="msg.content"
						:image="msg.imagePath || msg.image"
						:actionType="msg.actionType"
						:actionData="msg.actionData"
						@action-confirm="handleActionConfirm"
						@action-edit="handleActionEdit"
						@action-cancel="handleActionCancel"
					>
						<!-- 插槽：用于扩展特殊消息类型 -->
						<slot :name="'message-' + index" :msg="msg" :index="index"></slot>
					</ChatMessage>
				</view>
				
				<!-- AI 工作状态显示 -->
				<view v-if="loading" class="ai-status">
					<view class="status-icon">
						<view class="dot"></view>
						<view class="dot"></view>
						<view class="dot"></view>
					</view>
					<text class="status-text">{{ statusText }}</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import ChatMessage from '../components/ChatMessage.vue';

export default {
	name: 'ChatView',
	components: {
		ChatMessage
	},
	props: {
		messages: {
			type: Array,
			default: () => []
		},
		scrollToMsgId: {
			type: String,
			default: ''
		},
		loading: {
			type: Boolean,
			default: false
		},
		statusText: {
			type: String,
			default: '思考中...'
		}
	},
	methods: {
		loadMore() {
			this.$emit('load-more');
		},
		scrollToBottom() {
			if (this.messages.length > 0) {
				this.$emit('scroll-to', 'msg-' + (this.messages.length - 1));
			}
		},
		handleActionConfirm(event) {
			this.$emit('action-confirm', event);
		},
		handleActionEdit(event) {
			this.$emit('action-edit', event);
		},
		handleActionCancel(event) {
			this.$emit('action-cancel', event);
		}
	}
}
</script>

<style lang="scss" scoped>
.chat-view {
	flex: 1;
	height: 0;
	overflow: hidden;
}

.chat-main {
	height: 100%;
	
	.chat-content {
		padding: 30rpx;
		padding-bottom: 60rpx;
	}
	
	.ai-status {
		display: flex;
		align-items: center;
		gap: 16rpx;
		padding: 24rpx 30rpx;
		margin: 20rpx 0;
		background: rgba(99, 102, 241, 0.05);
		border-radius: 24rpx;
		width: fit-content;
		animation: fadeIn 0.3s ease;
		
		.status-icon {
			display: flex;
			gap: 8rpx;
			
			.dot {
				width: 16rpx;
				height: 16rpx;
				background: #6366F1;
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
		
		.status-text {
			font-size: 26rpx;
			color: #6366F1;
			font-weight: 500;
		}
	}
}

@keyframes bounce {
	0%, 80%, 100% {
		transform: scale(0);
		opacity: 0.5;
	}
	40% {
		transform: scale(1);
		opacity: 1;
	}
}

@keyframes fadeIn {
	from {
		opacity: 0;
		transform: translateY(10rpx);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}
</style>
