<template>
	<view class="chat-input">
		<view class="input-row">
			<view v-if="showAddBtn" class="add-btn" @click="$emit('add')">
				<image class="icon" src="/static/Health/plus-circle.svg"/>
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
				<view v-if="showMic" class="mic-btn" @click="$emit('voice')">
					<image class="icon" src="/static/ai/microphone.svg"/>
				</view>
			</view>
			<view class="send-btn" @click="handleSend">
				<image class="icon" src="/static/ai/send.svg"/>
			</view>
		</view>
		<view class="home-bar"></view>
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
		showAddBtn: {
			type: Boolean,
			default: true
		},
		showMic: {
			type: Boolean,
			default: true
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
		},
		clearInput() {
			this.inputValue = '';
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #3B82F6;

.chat-input {
	padding: 20rpx 30rpx;
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: blur(20px);
	border-top: 1rpx solid #e2e8f0;
	@media (prefers-color-scheme: dark) { 
		background: rgba(15, 23, 42, 0.9); 
		border-color: #1e293b; 
	}

	.input-row {
		display: flex; 
		align-items: center; 
		gap: 24rpx; 
		padding-bottom: 20rpx;
		
		.add-btn {
			display: flex;
			align-items: center;
			justify-content: center;
			
			.icon {
				width: 48rpx;
				height: 48rpx;
				color: #94a3b8;
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
				color: #1e293b; 
				@media (prefers-color-scheme: dark) { color: #fff; } 
			}
			
			.mic-btn {
				display: flex;
				align-items: center;
				justify-content: center;
				
				.icon {
					width: 40rpx;
					height: 40rpx;
					color: #94a3b8;
				}
			}
		}
		
		.send-btn {
			width: 84rpx; 
			height: 84rpx; 
			background: $primary; 
			border-radius: 50%;
			display: flex; 
			align-items: center; 
			justify-content: center; 
			color: #fff;
			box-shadow: 0 8rpx 20rpx rgba(59, 130, 246, 0.3);
			cursor: pointer;
			
			.icon {
				width: 36rpx;
				height: 36rpx;
			}
		}
	}
	
	.home-bar { 
		width: 240rpx; 
		height: 8rpx; 
		background: #e2e8f0; 
		border-radius: 100rpx; 
		margin: 10rpx auto 0; 
		@media (prefers-color-scheme: dark) { background: #334155; } 
	}
}
</style>
