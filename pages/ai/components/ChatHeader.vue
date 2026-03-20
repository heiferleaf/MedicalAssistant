<template>
	<view class="chat-header">
		<view class="header-left">
			<view v-if="showMenu" class="menu-btn" @click="toggleSidebar">
				<text class="menu-icon">☰</text>
			</view>
			<view class="header-info">
				<text class="header-title">{{ title }}</text>
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
		title: {
			type: String,
			default: '智愈助手'
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
	justify-content: center;
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
		
		.header-info { 
			display: flex;
			align-items: center;
			justify-content: center;
			flex: 1;
			
			.header-title { 
				font-size: 36rpx; 
				font-weight: 700;
				letter-spacing: 2rpx;
				background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
				-webkit-background-clip: text;
				-webkit-text-fill-color: transparent;
				background-clip: text;
			}
		}
	}
}
</style>
