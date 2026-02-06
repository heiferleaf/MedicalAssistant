<template>
	<view class="scan-container">
		<image class="camera-bg" src="https://tse4.mm.bing.net/th/id/OIP.2uGDA7bp8XHxtOYWjiKyogHaFL?cb=defcache2defcache=1&rs=1&pid=ImgDetMain&o=7&rm=3" mode="aspectFill"></image>
		
		<view class="mask">
			<view class="scan-window">
				<view class="corner top-left"></view>
				<view class="corner top-right"></view>
				<view class="corner bottom-left"></view>
				<view class="corner bottom-right"></view>
				<view class="scan-line"></view>
			</view>
		</view>

		<view class="ai-tags">
			<view class="tag-item floating-1">💊 药品名：阿司匹林...</view>
			<view class="tag-item floating-2">🕒 频率：每日一次</view>
		</view>

		<view class="scan-footer">
			<text class="tips">请将包装上的“用法用量”对准框内</text>
			<view class="action-bar">
				<view class="icon-btn"><uni-icons type="image" size="28" color="#fff"></uni-icons></view>
				<view class="shutter-outer">
					<view class="shutter-inner"></view>
				</view>
				<view class="icon-btn"><uni-icons type="refreshtween" size="28" color="#fff"></uni-icons></view>
			</view>
		</view>
	</view>
</template>

<style lang="scss">
.scan-container {
	height: 100vh; position: relative; background: #000;
	.camera-bg { width: 100%; height: 100%; opacity: 0.7; }
	
	.mask {
		position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
		background: rgba(0,0,0,0.4);
		.scan-window {
			width: 500rpx; height: 500rpx; position: relative;
			box-shadow: 0 0 0 1000rpx rgba(0,0,0,0.5);
			.corner {
				position: absolute; width: 40rpx; height: 40rpx; border: 6rpx solid #007AFF;
				&.top-left { top: 0; left: 0; border-right: 0; border-bottom: 0; }
				&.top-right { top: 0; right: 0; border-left: 0; border-bottom: 0; }
				&.bottom-left { bottom: 0; left: 0; border-right: 0; border-top: 0; }
				&.bottom-right { bottom: 0; right: 0; border-left: 0; border-top: 0; }
			}
			.scan-line {
				position: absolute; width: 100%; height: 2px; background: linear-gradient(to right, transparent, #007AFF, transparent);
				animation: scanMove 2s infinite;
			}
		}
	}

	.ai-tags {
		position: absolute; top: 20%; width: 100%;
		.tag-item {
			position: absolute; background: rgba(0,122,255,0.8); color: #fff;
			padding: 8rpx 20rpx; border-radius: 40rpx; font-size: 24rpx; backdrop-filter: blur(4px);
		}
		.floating-1 { left: 15%; top: 50rpx; animation: float 3s infinite; }
		.floating-2 { right: 10%; top: 150rpx; animation: float 4s infinite; }
	}

	.scan-footer {
		position: absolute; bottom: 80rpx; width: 100%; text-align: center;
		.tips { color: #fff; font-size: 26rpx; letter-spacing: 2rpx; margin-bottom: 40rpx; display: block; }
		.action-bar {
			display: flex; justify-content: space-evenly; align-items: center;
			.shutter-outer {
				width: 140rpx; height: 140rpx; border: 8rpx solid #fff; border-radius: 50%;
				display: flex; align-items: center; justify-content: center;
				.shutter-inner { width: 110rpx; height: 110rpx; background: #fff; border-radius: 50%; }
			}
		}
	}
}
@keyframes scanMove { 0% { top: 0; } 100% { top: 100%; } }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-20rpx); } }
</style>