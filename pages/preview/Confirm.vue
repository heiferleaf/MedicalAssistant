<template>
	<view class="confirm-page">
		<view class="top-card">
			<image class="mini-source" src="https://ui-avatars.com/api/?name=AI&background=007AFF&color=fff" mode="aspectFill"></image>
			<view class="result-brief">
				<text class="status-tag">智能解析完成</text>
				<text class="main-title">请核对用药信息</text>
			</view>
		</view>

		<view class="form-container">
			<view class="form-item">
				<text class="label">药品名称</text>
				<input class="input" value="阿司匹林肠溶片" />
			</view>

			<view class="form-item warning">
				<view class="label-row">
					<text class="label">单次剂量</text>
					<text class="warn-text">需人工确认</text>
				</view>
				<view class="input-wrap">
					<input class="input highlight" value="100mg (疑似)" />
					<text class="edit-link">修改</text>
				</view>
			</view>

			<view class="form-item">
				<text class="label">每日次数</text>
				<view class="tag-selector">
					<view class="tag active">每日1次</view>
					<view class="tag">每日2次</view>
					<view class="tag">遵医嘱</view>
				</view>
			</view>

			<uni-collapse>
				<uni-collapse-item title="查看照片提取的原始文本" title-style="font-size:24rpx;color:#999;">
					<view class="raw-content">
						"用法用量：口服。一日1次，一次1片（100mg），建议晚餐后用温开水送服。请密封保存..."
					</view>
				</uni-collapse-item>
			</uni-collapse>
		</view>

		<view class="bottom-bar">
			<button class="btn-cancel">重新拍摄</button>
			<button class="btn-confirm shadow-blue">确认并开启提醒</button>
		</view>
	</view>
</template>

<style lang="scss">
.confirm-page {
	background: #F4F7FA; min-height: 100vh; padding-bottom: 180rpx;
	
	.top-card {
		background: #fff; padding: 60rpx 40rpx; display: flex; align-items: center;
		border-radius: 0 0 40rpx 40rpx;
		.mini-source { width: 120rpx; height: 120rpx; border-radius: 16rpx; margin-right: 30rpx; }
		.status-tag { font-size: 20rpx; color: #007AFF; background: rgba(0,122,255,0.1); padding: 4rpx 12rpx; border-radius: 8rpx; }
		.main-title { display: block; font-size: 36rpx; font-weight: bold; margin-top: 10rpx; }
	}

	.form-container {
		padding: 30rpx;
		.form-item {
			background: #fff; border-radius: 24rpx; padding: 30rpx; margin-bottom: 20rpx;
			.label { font-size: 26rpx; color: #666; margin-bottom: 16rpx; display: block; }
			.input { font-size: 30rpx; font-weight: 500; color: #333; }
			
			&.warning {
				border: 2rpx solid #FFCC00; background: #FFFCF0;
				.label-row { display: flex; justify-content: space-between; align-items: center; }
				.warn-text { font-size: 20rpx; color: #E6A23C; font-weight: bold; }
				.input-wrap { display: flex; justify-content: space-between; align-items: center; }
				.highlight { color: #E6A23C; }
				.edit-link { color: #007AFF; font-size: 24rpx; font-weight: bold; }
			}
		}
	}

	.tag-selector {
		display: flex; gap: 20rpx;
		.tag { 
			padding: 12rpx 30rpx; border-radius: 12rpx; background: #F0F2F5; font-size: 24rpx;
			&.active { background: #007AFF; color: #fff; }
		}
	}

	.raw-content { padding: 20rpx; font-size: 24rpx; color: #999; line-height: 1.6; background: #fdfdfd; }

	.bottom-bar {
		position: fixed; bottom: 0; width: 100%; background: #fff; padding: 30rpx 40rpx 60rpx;
		display: flex; gap: 30rpx; box-sizing: border-box;
		.btn-cancel { flex: 1; height: 90rpx; line-height: 90rpx; border-radius: 45rpx; font-size: 28rpx; background: #F0F2F5; color: #666; }
		.btn-confirm { flex: 2; height: 90rpx; line-height: 90rpx; border-radius: 45rpx; font-size: 28rpx; background: #007AFF; color: #fff; }
		.shadow-blue { box-shadow: 0 10rpx 20rpx rgba(0,122,255,0.3); }
	}
}
</style>