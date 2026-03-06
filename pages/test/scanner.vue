<template>
	<view class="scanner-page">
		<view class="camera-section">
			<camera device-position="back" flash="auto" @error="onCameraError" class="camera-view">
				<view class="scan-mask">
					<view class="scan-area" :class="{ 'focusing': isClear }"></view>
				</view>
				
				<view class="status-indicator" :class="isClear ? 'safe' : 'warn'">
					<text class="iconfont">{{ isClear ? '' : '' }}</text>
					<text>{{ isClear ? '光照环境良好，请拍摄' : '环境太暗或画面模糊' }}</text>
				</view>
			</camera>
		</view>

		<view class="control-panel">
			<view class="tip-text">请将药盒正面放入框内，自动开启视觉纠偏</view>
			
			<view class="btn-group">
				<button class="capture-btn" @tap="takePhoto" :disabled="uploading">
					<view class="inner-circle"></view>
				</button>
			</view>
		</view>

		<uni-popup ref="resultPopup" type="bottom" background-color="#fff">
			<view class="result-card">
				<view class="card-header">
					<text class="title">AI 识别解析结果</text>
					<text class="confidence">置信度: 98.2%</text>
				</view>
				
				<view class="med-info" v-if="parsedData">
					<view class="info-row">
						<text class="label">药品名称</text>
						<text class="val">{{ parsedData.name }}</text>
						<text class="correction-tag">已纠错</text>
					</view>
					<view class="info-row">
						<text class="label">推荐剂量</text>
						<text class="val">{{ parsedData.dosage }}</text>
					</view>
					<view class="info-row">
						<text class="label">服用频次</text>
						<text class="val-bold">{{ parsedData.frequency }}</text>
					</view>
				</view>
				
				<button class="confirm-btn" @tap="closePopup">同步至服药计划</button>
			</view>
		</uni-popup>
	</view>
</template>

<script>
export default {
	data() {
		return {
			isClear: true, // 模拟 Laplacian 算子实时评价结果
			uploading: false,
			parsedData: null
		}
	},
	methods: {
		// 模拟拍照并上传
		async takePhoto() {
			const ctx = uni.createCameraContext();
			ctx.takePhoto({
				quality: 'high',
				success: (res) => {
					this.processAndUpload(res.tempImagePath);
				}
			});
		},
		
		async processAndUpload(path) {
			this.uploading = true;
			uni.showLoading({ title: '视觉纠偏中...' });

			// 模拟图像预处理时间 (CLAHE对比度增强等)
			await new Promise(resolve => setTimeout(resolve, 800));

			uni.showLoading({ title: '多模态引擎解析中...' });
			
			// 模拟请求 SpringBoot 接口
			// uni.uploadFile({ url: 'http://your-springboot-api/ocr/parse', ... })
			
			setTimeout(() => {
				this.uploading = false;
				uni.hideLoading();
				
				// 模拟解析出的结构化 JSON
				this.parsedData = {
					name: "布洛芬缓释胶囊",
					dosage: "0.3g (1粒)",
					frequency: "每日两次 (随餐)",
					raw_text: "建议每12小时服用一次..."
				};
				
				this.$refs.resultPopup.open();
			}, 1500);
		},
		
		closePopup() {
			this.$refs.resultPopup.close();
			uni.showToast({ title: '已加入日程', icon: 'success' });
		}
	}
}
</script>

<style lang="scss" scoped>
.scanner-page {
	height: 100vh;
	background: #000;
	display: flex;
	flex-direction: column;
}

.camera-section {
	flex: 1;
	position: relative;
	
	.camera-view {
		width: 100%;
		height: 100%;
	}
}

.scan-mask {
	position: absolute;
	inset: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(0,0,0,0.2);
	
	.scan-area {
		width: 500rpx;
		height: 500rpx;
		border: 2rpx solid rgba(255,255,255,0.5);
		border-radius: 40rpx;
		position: relative;
		transition: all 0.3s;
		
		&.focusing {
			border-color: #3B82F6;
			box-shadow: 0 0 30rpx rgba(59, 130, 246, 0.5);
		}
	}
}

.status-indicator {
	position: absolute;
	top: 100rpx;
	left: 50%;
	transform: translateX(-50%);
	padding: 16rpx 40rpx;
	border-radius: 100rpx;
	display: flex;
	align-items: center;
	gap: 12rpx;
	font-size: 24rpx;
	backdrop-filter: blur(10px);
	
	&.safe { background: rgba(34, 197, 94, 0.2); color: #4ade80; }
	&.warn { background: rgba(239, 68, 68, 0.2); color: #f87171; }
}

.control-panel {
	height: 350rpx;
	background: #fff;
	border-radius: 60rpx 60rpx 0 0;
	padding: 40rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	
	.tip-text { color: #94a3b8; font-size: 26rpx; margin-bottom: 40rpx; }
}

.capture-btn {
	width: 140rpx; height: 140rpx; border: 8rpx solid #e2e8f0;
	border-radius: 50%; padding: 10rpx; background: transparent;
	
	.inner-circle { width: 100%; height: 100%; background: #3B82F6; border-radius: 50%; }
}

.result-card {
	padding: 50rpx;
	border-radius: 40rpx 40rpx 0 0;
	
	.card-header {
		display: flex; justify-content: space-between; margin-bottom: 40rpx;
		.title { font-size: 36rpx; font-weight: 800; color: #1e293b; }
		.confidence { font-size: 22rpx; color: #10b981; background: rgba(16,185,129,0.1); padding: 4rpx 12rpx; border-radius: 8rpx; }
	}
	
	.info-row {
		display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f1f5f9;
		.label { width: 160rpx; color: #64748b; font-size: 28rpx; }
		.val { flex: 1; color: #1e293b; font-size: 28rpx; }
		.val-bold { flex: 1; color: #3B82F6; font-size: 32rpx; font-weight: 700; }
		.correction-tag { font-size: 20rpx; color: #f59e0b; border: 1rpx solid #f59e0b; padding: 2rpx 8rpx; border-radius: 4rpx; }
	}
	
	.confirm-btn { margin-top: 50rpx; background: #3B82F6; color: #fff; border-radius: 24rpx; font-weight: 700; }
}
</style>