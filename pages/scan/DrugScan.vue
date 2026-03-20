<template>
	<view class="scan-container">
		<image 
			class="camera-mock" 
			src="https://lh3.googleusercontent.com/aida-public/AB6AXuBfONEhHBAmpy-4FBN77eb5FWS7NT1zqJ-g2aSujEg2Fv2mveCMhxN-yB3hz0T1xb86LUrlfoJugsT9KuojlLqWwJ7E0d5btBybu01U_-Vt5-f6UM5xQtAGHLEIajk2BQLDTHCzyDglf-B_Vf8a_jHgOpZFdH_xMHM9FGFEEBZZXY-TNtE5m4Xcz4Rm0D1qydgVRPKOzkFrC4uTiWy3943mM6h4gGb_ivRAolrIjBquJJRor1Ar-ydBEbaCM3hZu1qSxgGb24xM-GI" 
			mode="aspectFill"
		></image>
		<view class="overlay-gradient"></view>

		<view class="status-bar"></view>

		<view class="nav-header">
			<view class="icon-btn-blur" @tap="goBack">
				<image class="icon" src="/static/Health/close.svg"/>
			</view>
		</view>

		<view class="ai-info-layer">
			<view class="info-pill primary-pill pulse-anim">
				<image class="icon" src="/static/Health/pill-inactive.svg" style="height: 48rpx;width: 48rpx;"/>
				<text class="pill-text">药品名: 阿司匹林肠溶片</text>
			</view>
			<view class="info-pill secondary-pill">
				<image class="icon" src="/static/Prepare/schedule.svg" style="height: 48rpx;width: 48rpx;"/>
				<text class="pill-text">频率: 每日一次</text>
			</view>
		</view>

		<view class="scan-wrapper">
			<view class="scan-box">
				<view class="corner tl"></view>
				<view class="corner tr"></view>
				<view class="corner bl"></view>
				<view class="corner br"></view>
				<view class="scan-line"></view>
			</view>
			<view class="scan-tips">
				<text class="tip-title">请将包装上的“用法用量”对准框内</text>
				<text class="tip-sub">正在智能识别中...</text>
			</view>
		</view>

		<view class="bottom-controls">
			<view class="tool-row">
				<view class="tool-btn">
					<image class="icon" src="/static/DrugScan/gallery.svg" style="height: 56rpx;width: 56rpx;"/>
				</view>
				
				<view class="shutter-btn" @tap="handleCapture()">
					<view class="shutter-inner"></view>
				</view>
				
				<view class="tool-btn">
					<image class="icon" src="/static/DrugScan/flash-on.svg" style="height: 56rpx;width: 56rpx;"/>
				</view>
			</view>

			<view class="mode-tabs">
				<text class="mode-item">扫码添加</text>
				<view class="mode-item active">
					<text>AI 识别</text>
					<view class="active-line"></view>
				</view>
				<text class="mode-item">手动录入</text>
			</view>
		</view>

		<view class="home-indicator"></view>
	</view>
</template>

<script>
export default {
	methods: {
		goBack() {
			uni.navigateBack();
		},
		handleCapture() {
			// 跳转逻辑：模拟拍摄后进入分析页面
			uni.navigateTo({
				url: '/pages/scan/Analysis',
				animationType: 'fade-in',
				animationDuration: 300
			});
		}
	}
}
</script>

<style lang="scss" scoped>
.scan-container {
	width: 100vw;
	height: 100vh;
	background-color: #000;
	position: relative;
	overflow: hidden;
}

.camera-mock {
	position: absolute;
	width: 100%;
	height: 100%;
	opacity: 0.6;
}

.overlay-gradient {
	position: absolute;
	width: 100%;
	height: 100%;
	background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, transparent 40%, rgba(0,0,0,0.4) 100%);
}

.status-bar {
	height: var(--status-bar-height);
}

.nav-header {
	padding: 20rpx 40rpx;
	.icon-btn-blur {
		width: 80rpx;
		height: 80rpx;
		background: rgba(255, 255, 255, 0.1);
		backdrop-filter: blur(8px);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
	}
}

.ai-info-layer {
	padding: 60rpx;
	.info-pill {
		display: flex;
		align-items: center;
		padding: 16rpx 32rpx;
		border-radius: 100rpx;
		backdrop-filter: blur(10px);
		margin-bottom: 24rpx;
		width: fit-content;
		
		&.primary-pill {
			background: rgba(59, 130, 246, 0.9);
			box-shadow: 0 8rpx 20rpx rgba(59, 130, 246, 0.3);
			transform: rotate(-1deg);
		}
		&.secondary-pill {
			background: rgba(79, 70, 229, 0.9);
			margin-left: 60rpx;
		}
		
		.emoji { margin-right: 12rpx; font-size: 28rpx; }
		.pill-icon { font-size: 24rpx; margin-right: 12rpx; color: #fff; }
		.pill-text { color: #fff; font-size: 26rpx; font-weight: 500; }
	}
}

.scan-wrapper {
	position: absolute;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -45%);
	display: flex;
	flex-direction: column;
	align-items: center;

	.scan-box {
		width: 500rpx;
		height: 500rpx;
		border: 2rpx solid rgba(255, 255, 255, 0.1);
		position: relative;
		border-radius: 24rpx;

		.corner {
			position: absolute;
			width: 40rpx;
			height: 40rpx;
			border-color: #3B82F6;
			border-style: solid;
			&.tl { top: -4rpx; left: -4rpx; border-width: 8rpx 0 0 8rpx; border-top-left-radius: 16rpx; }
			&.tr { top: -4rpx; right: -4rpx; border-width: 8rpx 8rpx 0 0; border-top-right-radius: 16rpx; }
			&.bl { bottom: -4rpx; left: -4rpx; border-width: 0 0 8rpx 8rpx; border-bottom-left-radius: 16rpx; }
			&.br { bottom: -4rpx; right: -4rpx; border-width: 0 8rpx 8rpx 0; border-bottom-right-radius: 16rpx; }
		}

		.scan-line {
			position: absolute;
			width: 100%;
			height: 4rpx;
			background: linear-gradient(90deg, transparent, #3B82F6, transparent);
			box-shadow: 0 0 20rpx #3B82F6;
			animation: scanMove 3s infinite linear;
		}
	}
	
	.scan-tips {
		text-align: center;
		margin-top: 60rpx;
		.tip-title { color: #fff; font-size: 34rpx; font-weight: 500; text-shadow: 0 2rpx 10rpx rgba(0,0,0,0.5); display: block; }
		.tip-sub { color: rgba(255,255,255,0.6); font-size: 24rpx; margin-top: 16rpx; }
	}
}

.bottom-controls {
	position: absolute;
	bottom: 80rpx;
	width: 100%;
	
	.tool-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0 100rpx;
		margin-bottom: 60rpx;
		
		.tool-btn {
			width: 100rpx;
			height: 100rpx;
			background: rgba(255,255,255,0.1);
			backdrop-filter: blur(8px);
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			color: #fff;
			.material-icons { font-size: 48rpx; }
		}
		
		.shutter-btn {
			width: 160rpx;
			height: 160rpx;
			border: 8rpx solid #fff;
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			.shutter-inner {
				width: 130rpx;
				height: 130rpx;
				background: #fff;
				border-radius: 50%;
			}
			&:active { transform: scale(0.95); transition: 0.1s; }
		}
	}

	.mode-tabs {
		display: flex;
		justify-content: center;
		gap: 60rpx;
		.mode-item {
			color: rgba(255,255,255,0.5);
			font-size: 28rpx;
			font-weight: 600;
			position: relative;
			&.active {
				color: #fff;
				.active-line {
					position: absolute;
					bottom: -12rpx;
					left: 0;
					width: 100%;
					height: 4rpx;
					background: #3B82F6;
				}
			}
		}
	}
}

.home-indicator {
	position: absolute;
	bottom: 20rpx;
	left: 50%;
	transform: translateX(-50%);
	width: 240rpx;
	height: 8rpx;
	background: rgba(255,255,255,0.3);
	border-radius: 100rpx;
}

@keyframes scanMove {
	0% { top: 0; opacity: 0; }
	50% { opacity: 1; }
	100% { top: 500rpx; opacity: 0; }
}

@keyframes pulse {
	0% { transform: scale(1) rotate(-1deg); }
	50% { transform: scale(1.05) rotate(-1deg); }
	100% { transform: scale(1) rotate(-1deg); }
}

.pulse-anim {
	animation: pulse 2s infinite ease-in-out;
}

.material-icons {
	font-family: 'Material Icons';
	font-size: 40rpx;
}
</style>