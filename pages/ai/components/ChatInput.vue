<template>
	<view class="chat-input">
		<!-- 大圆角输入框容器 -->
		<view class="input-container">
			<!-- 左侧：相机按钮 -->
			<view class="camera-btn" @click="$emit('camera')">
				<image class="icon" src="/static/ai/camera.svg"/>
			</view>
			
			<!-- 中间：输入框 -->
			<view class="input-box">
				<input 
					v-model="inputValue" 
					class="real-input" 
					:placeholder="placeholder"
					placeholder-class="ph"
					@confirm="handleSend"
					:focus="focus"
				/>
			</view>
			
			<!-- 右侧：联网按钮 -->
			<view class="network-btn" :class="{ 'active': isNetworkOn }" @click="toggleNetwork">
				<image class="icon" src="/static/ai/network.svg"/>
			</view>
			
			<!-- 最右侧：发送按钮 -->
			<view class="send-btn" @click="handleSend">
				<image class="icon" src="/static/ai/send.svg"/>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'ChatInput',
	props: {
		placeholder: {
			type: String,
			default: '输入健康疑问...'
		},
		focus: {
			type: Boolean,
			default: false
		}
	},
	data() {
		return {
			inputValue: '',
			isNetworkOn: false  // 联网状态，默认关闭
		}
	},
	methods: {
		// 切换联网状态
		toggleNetwork() {
			this.isNetworkOn = !this.isNetworkOn;
			uni.showToast({
				title: this.isNetworkOn ? '已开启联网' : '已关闭联网',
				icon: 'none',
				duration: 1500
			});
		},
		
		handleSend() {
			if (!this.inputValue.trim()) return;
			this.$emit('send', this.inputValue.trim());
			this.inputValue = '';
		}
	}
}
</script>

<style lang="scss" scoped>
.chat-input {
	position: relative;
	background: transparent;
	padding: 20rpx 30rpx;
	
	@media (prefers-color-scheme: dark) { 
		background: transparent;
	}
	
	// 大圆角输入框容器：悬浮样式
	.input-container {
		background: #ffffff;
		border-radius: 48rpx;
		padding: 16rpx 24rpx;
		display: flex;
		align-items: center;
		gap: 16rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
		
		@media (prefers-color-scheme: dark) { 
			background: #1e293b;
			box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.3);
		}
		
		// 相机按钮
		.camera-btn {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 64rpx;
			height: 64rpx;
			flex-shrink: 0;
			
			.icon {
				width: 44rpx;
				height: 44rpx;
				color: #64748b;
			}
			
			&:active {
				transform: scale(0.95);
			}
		}
		
		// 输入框
		.input-box {
			flex: 1;
			display: flex;
			align-items: center;
			min-width: 0;  // 防止 flex 子项撑开
			
			.real-input {
				flex: 1;
				width: 100%;
				font-size: 30rpx;
				color: #333;
				@media (prefers-color-scheme: dark) { color: #f1f5f9; }
			}
			
			.ph {
				color: #94a3b8;
			}
		}
		
		// 联网按钮
		.network-btn {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 64rpx;
			height: 64rpx;
			flex-shrink: 0;
			border-radius: 50%;
			transition: all 0.3s ease;
			
			.icon {
				width: 48rpx;
				height: 48rpx;
				color: #64748b;
				transition: all 0.3s ease;
				filter: none;
			}
			
			// 激活状态：高亮显示
			&.active {
				background: rgba(59, 130, 246, 0.15);
				
				.icon {
					color: #2563eb;
					filter: none;
				}
			}
			
			&:active {
				transform: scale(0.95);
			}
		}
		
		// 发送按钮
		.send-btn {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 80rpx;
			height: 80rpx;
			background: linear-gradient(135deg, #6366f1, #8b5cf6);
			border-radius: 50%;
			flex-shrink: 0;
			box-shadow: 0 4rpx 12rpx rgba(99, 102, 241, 0.3);
			
			.icon {
				width: 40rpx;
				height: 40rpx;
				filter: brightness(0) invert(1);
			}
			
			&:active {
				transform: scale(0.95);
			}
		}
	}
}
</style>
