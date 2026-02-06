<template>
	<view class="analysis-container">
		<view class="status-bar"></view>

		<header class="header">
			<view class="ai-logo">
				<text class="ai-text">AI</text>
			</view>
			<view class="header-content">
				<view class="status-tag">智能解析完成</view>
				<text class="title">请核对用药信息</text>
			</view>
		</header>

		<scroll-view scroll-y class="content-body">
			<view class="padding-box">
				<view class="info-card">
					<text class="label">药品名称</text>
					<text class="value-lg">阿司匹林肠溶片</text>
				</view>

				<view class="info-card warning-card">
					<view class="card-row">
						<view>
							<text class="label color-amber">单次剂量</text>
							<view class="dose-row">
								<text class="value-xl color-amber">100mg</text>
								<text class="value-sub color-amber">(疑似)</text>
							</view>
						</view>
						<view class="card-right">
							<view class="warning-tag">
								<image class="icon" src="../../static/Home/warning.svg"/>
								<text class="tag-txt">需人工确认</text>
							</view>
							<text class="edit-btn" @tap="editField('dose')">修改</text>
						</view>
					</view>
				</view>

				<view class="info-card">
					<text class="label">每日次数</text>
					<view class="btn-group">
						<view 
							v-for="(item, index) in frequencyOptions" 
							:key="index"
							:class="['btn-item', currentFreq === index ? 'btn-active' : '']"
							@tap="currentFreq = index"
						>
							<text class="btn-text">{{item}}</text>
						</view>
					</view>
				</view>

				<view class="info-card">
					<view class="label-row">
						<text class="material-icons-round sm-icon">receipt_long</text>
						<text class="label">原始说明文本</text>
					</view>
					<scroll-view scroll-y class="raw-text-scroll">
						<text class="raw-text">
							"用法用量：口服。一日1次，一次1片（100mg），建议晚餐后用温开水送服。请密封保存，避免儿童误服。对于不耐受患者可考虑减少剂量..."
						</text>
					</scroll-view>
				</view>
			</view>
			
			<view class="safe-area-bottom"></view>
		</scroll-view>

		<image class="material-icons-round bg-decor" src="../../static/Mine/report.svg"/>

		<footer class="footer">
			<button class="primary-btn" @tap="confirmAndSave">
				<text>确认并开启提醒</text>
				<image class="icon" src="../../static/Home/bell.svg"/>
			</button>
			<button class="secondary-btn" @tap="reCapture">重新拍摄</button>
			<view class="ios-indicator"></view>
		</footer>
	</view>
</template>

<script>
export default {
	data() {
		return {
			currentFreq: 0,
			frequencyOptions: ['每日1次', '每日2次', '遵医嘱']
		}
	},
	methods: {
		editField(field) {
			uni.showToast({ title: '调起修改弹窗', icon: 'none' });
		},
		confirmAndSave() {
			uni.showToast({ title: '计划已开启', icon: 'success' });
			setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 1500);
		},
		reCapture() {
			uni.navigateBack();
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #3B82F6;
$bg-dark: #0F172A;

.analysis-container {
	min-height: 100vh;
	background-color: #F8FAFC;
	position: relative;
	display: flex;
	flex-direction: column;
	@media (prefers-color-scheme: dark) { background-color: $bg-dark; }
}

.status-bar { height: var(--status-bar-height); }

.header {
	padding: 40rpx 48rpx;
	display: flex;
	align-items: center;
	gap: 32rpx;
	
	.ai-logo {
		width: 96rpx;
		height: 96rpx;
		background: linear-gradient(135deg, #3B82F6, #2563EB);
		border-radius: 32rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 12rpx 24rpx rgba(59, 130, 246, 0.3);
		.ai-text { color: #fff; font-weight: 700; font-size: 36rpx; }
	}
	
	.status-tag {
		display: inline-block;
		padding: 4rpx 16rpx;
		background: rgba(59, 130, 246, 0.1);
		color: $primary;
		font-size: 24rpx;
		border-radius: 100rpx;
		margin-bottom: 8rpx;
	}
	
	.title {
		display: block;
		font-size: 40rpx;
		font-weight: 700;
		color: #1e293b;
		@media (prefers-color-scheme: dark) { color: #fff; }
	}
}

.content-body {
	flex: 1;
	.padding-box { padding: 0 48rpx; }
}

.info-card {
	background: #fff;
	padding: 40rpx;
	border-radius: 40rpx;
	margin-bottom: 32rpx;
	border: 1rpx solid #f1f5f9;
	@media (prefers-color-scheme: dark) { 
		background: rgba(30, 41, 59, 0.5); 
		border-color: #1e293b; 
	}
	
	.label {
		font-size: 24rpx;
		color: #94a3b8;
		text-transform: uppercase;
		font-weight: 500;
		margin-bottom: 16rpx;
		display: block;
	}
	
	.value-lg {
		font-size: 36rpx;
		font-weight: 600;
		color: #1e293b;
		@media (prefers-color-scheme: dark) { color: #fff; }
	}
}

.warning-card {
	background: #fffbeb;
	border-color: #fef3c7;
	@media (prefers-color-scheme: dark) { background: rgba(251, 191, 36, 0.1); border-color: rgba(251, 191, 36, 0.2); }
	
	.card-row { display: flex; justify-content: space-between; align-items: flex-start; }
	.dose-row { display: flex; align-items: baseline; gap: 8rpx; }
	.value-xl { font-size: 48rpx; font-weight: 700; }
	.value-sub { font-size: 28rpx; opacity: 0.7; }
	.color-amber { color: #b45309; @media (prefers-color-scheme: dark) { color: #fbbf24; } }
	
	.card-right {
		text-align: right;
		.warning-tag {
			display: flex;
			align-items: center;
			gap: 4rpx;
			color: #b45309;
			font-size: 24rpx;
			font-weight: 600;
			margin-bottom: 16rpx;
			@media (prefers-color-scheme: dark) { color: #fbbf24; }
			.material-icons-round { font-size: 28rpx; }
		}
		.edit-btn { color: $primary; font-weight: 700; font-size: 28rpx; }
	}
}

.btn-group {
	display: flex;
	gap: 16rpx;
	.btn-item {
		flex: 1;
		padding: 24rpx 0;
		text-align: center;
		background: #fff;
		border: 1rpx solid #e2e8f0;
		border-radius: 24rpx;
		@media (prefers-color-scheme: dark) { background: #334155; border-color: #475569; }
		
		.btn-text { font-size: 28rpx; color: #64748b; font-weight: 600; @media (prefers-color-scheme: dark) { color: #cbd5e1; } }
		
		&.btn-active {
			background: $primary;
			border-color: $primary;
			box-shadow: 0 8rpx 16rpx rgba(59, 130, 246, 0.2);
			.btn-text { color: #fff; }
		}
	}
}

.label-row {
	display: flex;
	align-items: center;
	gap: 8rpx;
	margin-bottom: 16rpx;
	.sm-icon { font-size: 32rpx; color: #94a3b8; }
	.label { margin-bottom: 0; }
}

.raw-text-scroll {
	max-height: 200rpx;
	.raw-text {
		font-size: 28rpx;
		line-height: 1.6;
		color: #64748b;
		font-style: italic;
		@media (prefers-color-scheme: dark) { color: #94a3b8; }
	}
}

.bg-decor {
	position: absolute;
	top: 180rpx;
	right: 40rpx;
	font-size: 200rpx;
	color: $primary;
	opacity: 0.1;
	transform: rotate(15deg);
	pointer-events: none;
}

.footer {
	padding: 40rpx 48rpx 60rpx;
	background: linear-gradient(to top, #fff 80%, transparent);
	@media (prefers-color-scheme: dark) { background: linear-gradient(to top, $bg-dark 80%, transparent); }
	
	.primary-btn {
		width: 100%;
		height: 110rpx;
		background: linear-gradient(to right, #3B82F6, #2563EB);
		border-radius: 32rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 16rpx;
		color: #fff;
		font-size: 32rpx;
		font-weight: 700;
		box-shadow: 0 12rpx 32rpx rgba(59, 130, 246, 0.4);
		margin-bottom: 24rpx;
		border: none;
		&:active { transform: scale(0.98); opacity: 0.9; }
	}
	
	.secondary-btn {
		width: 100%;
		height: 110rpx;
		background: #f1f5f9;
		color: #64748b;
		border-radius: 32rpx;
		font-size: 30rpx;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
		border: none;
		@media (prefers-color-scheme: dark) { background: #1e293b; color: #94a3b8; }
	}
}

.ios-indicator {
	width: 120rpx;
	height: 10rpx;
	background: #e2e8f0;
	border-radius: 100rpx;
	margin: 40rpx auto 0;
	@media (prefers-color-scheme: dark) { background: #334155; }
}

.safe-area-bottom { height: 350rpx; }

.material-icons-round { font-family: 'Material Icons Round'; }
</style>