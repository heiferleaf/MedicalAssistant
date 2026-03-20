<template>
	<view :class="['message-group', role === 'user' ? 'user-message' : 'ai-message']">
		<view :class="['chat-bubble', role === 'user' ? 'chat-bubble-user' : 'chat-bubble-ai']">
			<!-- 文本消息 -->
			<text v-if="type === 'text'" class="msg-text">{{ content }}</text>
			
			<!-- 图片消息 -->
			<image v-if="image" class="msg-img" :src="image" mode="aspectFill"></image>
			
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
</template>

<script>
export default {
	name: 'ChatMessage',
	props: {
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
}

.chat-bubble {
	padding: 30rpx;
	max-width: 85%;
	border-radius: 32rpx;
	font-size: 28rpx;
	line-height: 1.6;
	
	&.chat-bubble-ai {
		background: #fff;
		border-bottom-left-radius: 8rpx;
		color: #1e293b;
		@media (prefers-color-scheme: dark) { background: #1e293b; color: #f1f5f9; }
	}
	
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
