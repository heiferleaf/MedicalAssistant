<template>
	<view class="chat-input">
		<view class="input-row">
			<!-- 相机按钮 -->
			<view class="camera-btn" @click="$emit('camera')">
				<image class="icon" src="/static/ai/camera.svg"/>
			</view>
			
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
			inputValue: ''
		}
	},
	methods: {
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
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: blur(20px);
	@media (prefers-color-scheme: dark) { 
		background: rgba(15, 23, 42, 0.9); 
	}
	
	.input-row {
		display: flex; 
		align-items: center; 
		gap: 24rpx; 
		padding: 20rpx 30rpx;
		
		.camera-btn {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 84rpx;
			height: 84rpx;
			flex-shrink: 0;
			
			.icon {
				width: 48rpx;
				height: 48rpx;
				color: #94a3b8;
			}
			
			&:active {
				transform: scale(0.95);
			}
		}
		
		.input-box {
			flex: 1; 
			background: #f1f5f9; 
			border-radius: 100rpx; 
			padding: 0 30rpx;
			display: flex; 
			align-items: center; 
			height: 84rpx;
			@media (prefers-color-scheme: dark) { background: #334155; }
			
			.real-input { 
				flex: 1; 
				font-size: 28rpx; 
				color: #333;
				@media (prefers-color-scheme: dark) { color: #f1f5f9; }
			}
			
			.ph {
				color: #94a3b8;
			}
		}
		
		.send-btn {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 84rpx;
			height: 84rpx;
			background: #3B82F6;
			border-radius: 50%;
			flex-shrink: 0;
			
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
