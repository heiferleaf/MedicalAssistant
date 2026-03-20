<template>
	<view class="health-container">
		<view class="padding"></view>
		<view class="status-bar"></view>

		<scroll-view scroll-y class="scroll-body">
			<header class="header-section">
				<view class="top-bar">
					<text class="page-title">健康数据</text>
					<view class="action-btns">
						<view class="icon-btn" @tap="openCalendar">
							<image class="icon" src="/static/Health/calendar.svg"/>
						</view>
						<view class="icon-btn" @tap="addData">
							<image class="icon" src="/static/Health/plus-circle.svg"/>
						</view>
					</view>
				</view>

				<view class="index-card">
					<view class="card-top">
						<view>
							<text class="card-label">本周健康指数</text>
							<view class="score-row">
								<text class="score-num">85</text>
								<text class="score-tag">良好</text>
							</view>
						</view>
						<view class="trend-icon">
							<image class="icon" src="/static/Home/trend-up.svg"/>
						</view>
					</view>
					
					<view class="card-bottom">
						<text class="trend-text">较上周提升了 12%</text>
						<view class="chart-mini">
							<view class="bar" style="height: 32rpx; opacity: 0.3;"></view>
							<view class="bar" style="height: 48rpx; opacity: 0.3;"></view>
							<view class="bar" style="height: 24rpx; opacity: 0.3;"></view>
							<view class="bar" style="height: 56rpx; opacity: 0.3;"></view>
							<view class="bar" style="height: 40rpx; background: #fff;"></view>
						</view>
					</view>
				</view>
			</header>

			<section class="stats-section">
				<view class="stats-grid">
					<view class="stat-card half">
						<view class="stat-header">
							<view class="icon-tag bg-rose">
								<image class="icon" src="/static/Home/heart.svg"/>
							</view>
							<text class="stat-label">心率</text>
						</view>
						<view class="stat-value">
							<text class="val-num">72</text>
							<text class="val-unit">bpm</text>
						</view>
						<text class="status-dot text-emerald">● 正常</text>
					</view>

					<view class="stat-card half">
						<view class="stat-header">
							<view class="icon-tag bg-blue">
								<image class="icon" src="/static/Prepare/blood pressure.svg"/>
							</view>
							<text class="stat-label">血压</text>
						</view>
						<view class="stat-value">
							<text class="val-num">120/80</text>
							<text class="val-unit">mmHg</text>
						</view>
						<text class="status-dot text-emerald">● 理想</text>
					</view>
				</view>

				<view class="stat-card full sleep-card">
					<view class="sleep-left">
						<view class="icon-tag-large bg-indigo">
							<image class="icon" src="/static/Prepare/sleep.svg" style="height: 64rpx;width:64rpx;"/>
						</view>
						<view class="sleep-info">
							<text class="stat-label">昨晚睡眠</text>
							<text class="val-num-md">7h 45m</text>
						</view>
					</view>
					<view class="sleep-right">
						<text class="efficiency">效率 92%</text>
						<view class="progress-bg">
							<view class="progress-fill" style="width: 92%;"></view>
						</view>
					</view>
				</view>

				<view class="stat-card full">
					<view class="stat-header space-between">
						<view class="flex-row">
							<view class="icon-tag bg-orange">
								<image class="icon" src="/static/Prepare/walk.svg" style="height: 48rpx;width: 48rpx;"/>
							</view>
							<text class="stat-label">今日步数</text>
						</view>
						<text class="goal-text">目标: 10,000</text>
					</view>
					<view class="step-content">
						<view class="stat-value">
							<text class="val-num-lg">8,432</text>
							<text class="val-unit">步</text>
						</view>
						<view class="progress-bg-lg">
							<view class="progress-fill-orange" style="width: 84%;"></view>
						</view>
					</view>
				</view>
			</section>
			
			<view class="safe-bottom-padding"></view>
		</scroll-view>

		<app-navbar :current="2" />
	</view>
</template>

<script>
export default {
	data() {
		return {
			// 数据可以在此动态绑定
		};
	},
	methods: {
		openCalendar() {
			uni.showToast({ title: '打开日历', icon: 'none' });
		},
		addData() {
			uni.showToast({ title: '添加数据', icon: 'none' });
		}
	}
}
</script>

<style lang="scss" scoped>
// 基础变量
$primary-color: #6366F1;
$bg-light: #F8FAFC;
$bg-dark: #0F172A;

.padding {
	height: 64rpx;
}
.health-container {
	min-height: 100vh;
	background-color: $bg-light;
	@media (prefers-color-scheme: dark) {
		background-color: $bg-dark;
	}
}

.status-bar {
	height: var(--status-bar-height);
	width: 100%;
}

.scroll-body {
	height: calc(100vh - var(--status-bar-height));
}

.header-section {
	padding: 40rpx 40rpx 20rpx;

	.top-bar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 50rpx;

		.page-title {
			font-size: 48rpx;
			font-weight: 700;
			color: #1e293b;
			@media (prefers-color-scheme: dark) { color: #fff; }
		}

		.action-btns {
			display: flex;
			gap: 24rpx;

			.icon-btn {
				width: 80rpx;
				height: 80rpx;
				background: #fff;
				border-radius: 24rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
				@media (prefers-color-scheme: dark) { background: #1e293b; }
				
				.material-icons {
					font-size: 40rpx;
					color: #64748b;
					@media (prefers-color-scheme: dark) { color: #94a3b8; }
				}
			}
		}
	}

	.index-card {
		background: $primary-color;
		padding: 50rpx;
		border-radius: 60rpx;
		color: #fff;
		box-shadow: 0 20rpx 40rpx rgba(99, 102, 241, 0.3);

		.card-top {
			display: flex;
			justify-content: space-between;
			margin-bottom: 30rpx;

			.card-label {
				font-size: 28rpx;
				color: rgba(255, 255, 255, 0.8);
				margin-bottom: 10rpx;
				display: block;
			}

			.score-row {
				display: flex;
				align-items: baseline;
				.score-num { font-size: 80rpx; font-weight: 700; }
				.score-tag { font-size: 32rpx; margin-left: 12rpx; font-weight: 500; }
			}

			.trend-icon {
				background: rgba(255, 255, 255, 0.2);
				padding: 16rpx;
				border-radius: 24rpx;
        height: 48rpx;
			}
		}

		.card-bottom {
			display: flex;
			justify-content: space-between;
			align-items: flex-end;
			
			.trend-text { font-size: 24rpx; color: rgba(255, 255, 255, 0.8); }
			
			.chart-mini {
				display: flex;
				gap: 8rpx;
				align-items: flex-end;
				.bar {
					width: 12rpx;
					border-radius: 10rpx;
					background: rgba(255, 255, 255, 0.4);
				}
			}
		}
	}
}

.stats-section {
	padding: 0 40rpx;
	display: flex;
	flex-direction: column;
	gap: 30rpx;

	.stats-grid {
		display: flex;
		gap: 30rpx;
		.half { flex: 1; }
	}

	.stat-card {
		background: #fff;
		padding: 40rpx;
		border-radius: 48rpx;
		border: 1rpx solid #f1f5f9;
		box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.02);
		@media (prefers-color-scheme: dark) {
			background: #1e293b;
			border-color: rgba(255, 255, 255, 0.05);
		}

		.stat-header {
			display: flex;
			align-items: center;
			gap: 16rpx;
			margin-bottom: 24rpx;
			&.space-between { justify-content: space-between; }
			.flex-row { display: flex; align-items: center; gap: 16rpx; }
		}

		.icon-tag {
			width: 64rpx;
			height: 64rpx;
			border-radius: 16rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			.material-icons { font-size: 36rpx; }
		}

		.stat-label { font-size: 28rpx; color: #64748b; font-weight: 500; }

		.stat-value {
			display: flex;
			align-items: baseline;
			gap: 8rpx;
			.val-num { font-size: 48rpx; font-weight: 700; color: #1e293b; @media (prefers-color-scheme: dark) { color: #fff; } }
			.val-num-md { font-size: 40rpx; font-weight: 700; color: #1e293b; @media (prefers-color-scheme: dark) { color: #fff; } }
			.val-num-lg { font-size: 64rpx; font-weight: 700; color: #1e293b; @media (prefers-color-scheme: dark) { color: #fff; } }
			.val-unit { font-size: 24rpx; color: #94a3b8; }
		}

		.status-dot { font-size: 24rpx; margin-top: 20rpx; display: block; font-weight: 600; }
	}
}

// 颜色辅助类
.bg-rose { background: #fff1f2; @media (prefers-color-scheme: dark) { background: rgba(225, 29, 72, 0.1); } }
.text-rose { color: #f43f5e; }
.bg-blue { background: #eff6ff; @media (prefers-color-scheme: dark) { background: rgba(59, 130, 246, 0.1); } }
.text-blue { color: #3b82f6; }
.bg-indigo { background: #0037eb; @media (prefers-color-scheme: dark) { background: rgba(9, 13, 225, 0.1); } }
.text-indigo { color: #6366f1; }
.bg-orange { background: #fff7ed; @media (prefers-color-scheme: dark) { background: rgba(249, 115, 22, 0.1); } }
.text-orange { color: #f97316; }
.text-emerald { color: #10b981; }

.sleep-card {
	display: flex;
	justify-content: space-between;
	align-items: center;
	.sleep-left { display: flex; align-items: center; gap: 30rpx; }
	.icon-tag-large {
		width: 96rpx;
		height: 96rpx;
		border-radius: 32rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		.material-icons { font-size: 48rpx; }
	}
	.sleep-right { text-align: right; }
	.efficiency { font-size: 24rpx; color: $primary-color; font-weight: 600; }
}

.progress-bg {
	width: 160rpx;
	height: 12rpx;
	background: #f1f5f9;
	border-radius: 10rpx;
	margin-top: 10rpx;
	overflow: hidden;
	@media (prefers-color-scheme: dark) { background: #334155; }
	.progress-fill { height: 100%; background: $primary-color; }
}

.progress-bg-lg {
	width: 100%;
	height: 24rpx;
	background: #f1f5f9;
	border-radius: 20rpx;
	margin-top: 20rpx;
	overflow: hidden;
	@media (prefers-color-scheme: dark) { background: #334155; }
	.progress-fill-orange { height: 100%; background: #fb923c; border-radius: 20rpx; }
}

.goal-text { font-size: 24rpx; color: #94a3b8; }
.safe-bottom-padding { height: 240rpx; }

// 图标字体库
.material-icons {
	font-family: 'Material Icons Round';
}
</style>