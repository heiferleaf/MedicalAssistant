<template>
	<view class="welcome-view">
		<view class="welcome-content">
			<view class="ai-avatar">
				<text>AI</text>
			</view>
			<text class="welcome-title">欢迎使用智愈助手</text>
			<text class="welcome-subtitle">您的私人健康顾问</text>
			
			<view class="feature-list">
				<view v-for="(feature, index) in features" :key="index" class="feature-item">
					<image class="feature-icon" :src="feature.icon"/>
					<text class="feature-text">{{ feature.text }}</text>
				</view>
			</view>
			
			<view class="quick-actions">
				<text class="actions-title">快速开始</text>
				<view class="action-buttons">
					<button 
						v-for="(action, index) in actions" 
						:key="index"
						class="action-btn"
						@click="$emit('action', action)"
					>
						{{ action.text }}
					</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'WelcomeView',
	props: {
		features: {
			type: Array,
			default: () => [
				{ icon: '/static/Health/pill-active.svg', text: '用药管理' },
				{ icon: '/static/Home/warning.svg', text: '健康咨询' },
				{ icon: '/static/Mine/report.svg', text: '就医准备' }
			]
		},
		actions: {
			type: Array,
			default: () => [
				{ type: 'consult', text: '健康咨询' },
				{ type: 'medicine', text: '用药指导' },
				{ type: 'report', text: '查看报告' }
			]
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #3B82F6;

.welcome-view {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	padding: 60rpx 40rpx;
	
	.welcome-content {
		text-align: center;
		max-width: 600rpx;
		
		.ai-avatar {
			width: 160rpx;
			height: 160rpx;
			background: #fff;
			color: $primary;
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			font-size: 64rpx;
			font-weight: bold;
			margin: 0 auto 40rpx;
			box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.2);
		}
		
		.welcome-title {
			font-size: 48rpx;
			font-weight: 700;
			color: #fff;
			display: block;
			margin-bottom: 16rpx;
		}
		
		.welcome-subtitle {
			font-size: 28rpx;
			color: rgba(255, 255, 255, 0.9);
			display: block;
			margin-bottom: 60rpx;
		}
		
		.feature-list {
			margin-bottom: 60rpx;
			
			.feature-item {
				display: flex;
				align-items: center;
				justify-content: center;
				gap: 16rpx;
				padding: 20rpx 0;
				
				.feature-icon {
					width: 48rpx;
					height: 48rpx;
				}
				
				.feature-text {
					font-size: 28rpx;
					color: #fff;
					font-weight: 500;
				}
			}
		}
		
		.quick-actions {
			.actions-title {
				font-size: 28rpx;
				color: rgba(255, 255, 255, 0.9);
				display: block;
				margin-bottom: 24rpx;
			}
			
			.action-buttons {
				display: flex;
				flex-direction: column;
				gap: 16rpx;
				
				.action-btn {
					background: rgba(255, 255, 255, 0.2);
					color: #fff;
					border: 1rpx solid rgba(255, 255, 255, 0.3);
					border-radius: 100rpx;
					padding: 24rpx 48rpx;
					font-size: 28rpx;
					font-weight: 600;
					backdrop-filter: blur(10px);
					transition: all 0.2s;
					
					&:active {
						background: rgba(255, 255, 255, 0.3);
						transform: scale(0.98);
					}
				}
			}
		}
	}
}
</style>
