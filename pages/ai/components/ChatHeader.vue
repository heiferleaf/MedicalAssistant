<template>
	<view class="chat-header">
		<view class="header-left">
			<view v-if="showMenu" class="menu-btn" @click="toggleSidebar">
				<text class="menu-icon">☰</text>
			</view>
			<view class="ai-avatar">{{ avatarText }}</view>
			<view class="header-info">
				<text class="header-title">{{ title }}</text>
				<view class="online-status">
					<view class="dot"></view>
					<text class="status-text">{{ statusText }}</text>
				</view>
			</view>
		</view>
		<view class="header-right">
			<slot name="actions"></slot>
		</view>
	</view>
</template>

<script>
export default {
	name: 'ChatHeader',
	props: {
		showMenu: {
			type: Boolean,
			default: true
		},
		avatarText: {
			type: String,
			default: 'AI'
		},
		title: {
			type: String,
			default: '智愈助手'
		},
		statusText: {
			type: String,
			default: '在线 | 为您的健康护航'
		}
	},
	methods: {
		toggleSidebar() {
			this.$emit('toggle-sidebar');
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #3B82F6;

.chat-header {
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
</style>
