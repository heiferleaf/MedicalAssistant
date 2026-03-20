<template>
	<view class="mine-container">
		<view class="status-bar"></view>

		<scroll-view scroll-y class="scroll-body">
			<header class="header-section">
				<view class="top-row">
					<text class="page-title">个人中心</text>
					<view class="settings-btn" @tap="uni.navigateTo({ url: '/pages/mine/Settings' })">
						<image class="icon" src="../../static/Mine/settings.svg" />
					</view>
				</view>

				<view class="user-card" @tap="toProfile">
					<view class="avatar-wrapper">
						<image class="avatar" :src="getAvatar()" mode="aspectFill"></image>
						<view class="online-status"></view>
					</view>
					<view class="user-info">
						<text class="user-name">{{ username }}</text>
						<text class="user-status">坚持服药第 124 天</text>
						<view class="badge">
							<text class="badge-text">LV.4 活跃用户</text>
						</view>
					</view>
					<image class="icon" src="../../static/Mine/right-arrow.svg" />
				</view>
			</header>

			<section class="stats-section">
				<view class="stat-box bg-indigo">
					<text class="stat-label">今日步数</text>
					<text class="stat-value color-indigo">8,432</text>
				</view>
				<view class="stat-box bg-emerald">
					<text class="stat-label">服药率</text>
					<text class="stat-value color-emerald">98%</text>
				</view>
				<view class="stat-box bg-rose">
					<text class="stat-label">心率</text>
					<text class="stat-value color-rose">72</text>
				</view>
			</section>

			<section class="menu-section">
				<view class="menu-group">
					<view class="menu-item" hover-class="hover-gray" @tap="navigateTo('history')">
						<view class="menu-left">
							<view class="menu-icon bg-blue-light">
								<image class="icon" src="../../static/Health/clock-history.svg" />
							</view>
							<text class="menu-text">服药记录</text>
						</view>
						<image class="icon" src="../../static/Mine/right-arrow.svg" />
					</view>
					<view class="divider"></view>
					<view class="menu-item" hover-class="hover-gray" @tap="navigateTo('report')">
						<view class="menu-left">
							<view class="menu-icon bg-purple-light">
								<image class="icon" src="../../static/Mine/report.svg" />
							</view>
							<text class="menu-text">健康报告</text>
						</view>
						<image class="icon" src="../../static/Mine/right-arrow.svg" />
					</view>
					<view class="divider"></view>
					<view class="menu-item" hover-class="hover-gray" @tap="navigateTo('doctor')">
						<view class="menu-left">
							<view class="menu-icon bg-orange-light">
								<image class="icon" src="../../static/Mine/doctor.svg" />
							</view>
							<text class="menu-text">我的医生</text>
						</view>
						<image class="icon" src="../../static/Mine/right-arrow.svg" />
					</view>
				</view>

				<view class="menu-group mt-4">
					<view class="menu-item" hover-class="hover-gray" @tap="navigateTo('help')">
						<view class="menu-left">
							<view class="menu-icon bg-slate-light">
								<image class="icon" src="../../static/Mine/help.svg" />
							</view>
							<text class="menu-text">帮助与反馈</text>
						</view>
						<image class="icon" src="../../static/Mine/right-arrow.svg" />
					</view>
				</view>
			</section>

			<button class="btn-manage" hover-class="btn-manage-hover" @click="logout">
				退出登录
			</button>

			<view class="safe-area-bottom"></view>
		</scroll-view>

		<app-navbar :current="3" />
	</view>
</template>

<script>
export default {
	data() {
		return {
			username: "小明",
		};
	},
	mounted() {
		this.initData();
	},
	methods: {
		initData() {
			const username = uni.getStorageSync("username") || "小明";
			this.username = username;
		},
				getAvatar() {
			const userId = uni.getStorageSync("userId") || "defaultUser";
			return `https://api.dicebear.com/7.x/adventurer/svg?seed=${userId}`;
		},
		toSettings() {
			uni.navigateTo({ url: '/pages/mine/settings' });
		},
		toProfile() {
			uni.navigateTo({ url: '/pages/mine/profile' });
		},
		navigateTo(type) {
			console.log('跳转至：', type);
		},
		logout() {

			uni.showModal({
				title: '确认退出',
				content: '您确定要退出登录吗？',
				success: (res) => {
					if (res.confirm) {
						uni.clearStorageSync(); // 清除 userId 和双 Token
						uni.reLaunch({ url: "/pages/Login" });
					}
				},
				showCancel: true,
				cancelText: '取消',
				confirmText: '退出'
			});
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #4d88ff;
$bg-light: #F8FAFC;
$bg-dark: #0F172A;

.mine-container {
	min-height: 100vh;
	background-color: $bg-light;

	@media (prefers-color-scheme: dark) {
		background-color: $bg-dark;
	}
}

.status-bar {
	height: var(--status-bar-height);
}

.header-section {
	padding: 40rpx 40rpx 30rpx;

	.top-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 50rpx;

		.page-title {
			font-size: 48rpx;
			font-weight: 700;
			color: #1e293b;

			@media (prefers-color-scheme: dark) {
				color: #fff;
			}
		}

		.settings-btn {
			width: 80rpx;
			height: 80rpx;
			background: #fff;
			border-radius: 24rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);

			@media (prefers-color-scheme: dark) {
				background: #1e293b;
			}

			.material-icons {
				color: #64748b;
				font-size: 40rpx;
			}
		}
	}
}

.user-card {
	background: #fff;
	padding: 40rpx;
	border-radius: 50rpx;
	display: flex;
	align-items: center;
	gap: 30rpx;
	border: 1rpx solid #f1f5f9;

	@media (prefers-color-scheme: dark) {
		background: #1e293b;
		border-color: rgba(255, 255, 255, 0.05);
	}

	.avatar-wrapper {
		position: relative;

		.avatar {
			width: 140rpx;
			height: 140rpx;
			border-radius: 70rpx;
			background: #ffedd5;
			border: 6rpx solid $bg-light;

			@media (prefers-color-scheme: dark) {
				border-color: #334155;
			}
		}

		.online-status {
			position: absolute;
			bottom: 6rpx;
			right: 6rpx;
			width: 32rpx;
			height: 32rpx;
			background: #10b981;
			border: 4rpx solid #fff;
			border-radius: 50%;

			@media (prefers-color-scheme: dark) {
				border-color: #1e293b;
			}
		}
	}

	.user-info {
		flex: 1;

		.user-name {
			font-size: 40rpx;
			font-weight: 700;
			color: #1e293b;
			display: block;

			@media (prefers-color-scheme: dark) {
				color: #fff;
			}
		}

		.user-status {
			font-size: 26rpx;
			color: #64748b;
			margin-top: 4rpx;
			display: block;
		}

		.badge {
			display: inline-block;
			background: rgba(99, 102, 241, 0.1);
			padding: 4rpx 16rpx;
			border-radius: 10rpx;
			margin-top: 12rpx;

			.badge-text {
				color: $primary;
				font-size: 20rpx;
				font-weight: 700;
			}
		}
	}

	.arrow {
		color: #cbd5e1;
	}
}

.stats-section {
	padding: 0 40rpx;
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 20rpx;
	margin-top: 20rpx;

	.stat-box {
		padding: 30rpx 20rpx;
		border-radius: 40rpx;
		text-align: center;

		.stat-label {
			font-size: 22rpx;
			color: #64748b;
			display: block;
			margin-bottom: 8rpx;
		}

		.stat-value {
			font-size: 36rpx;
			font-weight: 700;
		}
	}
}

// 统计配色
.bg-indigo {
	background: #eef2ff;

	@media (prefers-color-scheme: dark) {
		background: rgba(99, 102, 241, 0.1);
	}
}

.color-indigo {
	color: #4f46e5;
}

.bg-emerald {
	background: #ecfdf5;

	@media (prefers-color-scheme: dark) {
		background: rgba(16, 185, 129, 0.1);
	}
}

.color-emerald {
	color: #059669;
}

.bg-rose {
	background: #fff1f2;

	@media (prefers-color-scheme: dark) {
		background: rgba(244, 63, 94, 0.1);
	}
}

.color-rose {
	color: #e11d48;
}

.menu-section {
	padding: 40rpx;

	.menu-group {
		background: #fff;
		border-radius: 50rpx;
		overflow: hidden;
		border: 1rpx solid #f1f5f9;

		@media (prefers-color-scheme: dark) {
			background: #1e293b;
			border-color: rgba(255, 255, 255, 0.05);
		}
	}

	.menu-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 36rpx 40rpx;
		transition: background 0.2s;

		.menu-left {
			display: flex;
			align-items: center;
			gap: 30rpx;

			.menu-text {
				font-size: 30rpx;
				font-weight: 500;
				color: #1e293b;

				@media (prefers-color-scheme: dark) {
					color: #f1f5f9;
				}
			}
		}

		.menu-icon {
			width: 80rpx;
			height: 80rpx;
			border-radius: 24rpx;
			display: flex;
			align-items: center;
			justify-content: center;

			.material-icons {
				font-size: 40rpx;
			}
		}

		.arrow-sm {
			color: #cbd5e1;
			font-size: 40rpx;
		}
	}

	.divider {
		height: 1rpx;
		background: #f1f5f9;
		margin: 0 40rpx;

		@media (prefers-color-scheme: dark) {
			background: rgba(255, 255, 255, 0.05);
		}
	}
}

// 菜单配色
.bg-blue-light {
	background: #eff6ff;

	@media (prefers-color-scheme: dark) {
		background: rgba(59, 130, 246, 0.1);
	}
}

.color-blue {
	color: #3b82f6;
}

.bg-purple-light {
	background: #faf5ff;

	@media (prefers-color-scheme: dark) {
		background: rgba(168, 85, 247, 0.1);
	}
}

.color-purple {
	color: #a855f7;
}

.bg-orange-light {
	background: #fff7ed;

	@media (prefers-color-scheme: dark) {
		background: rgba(249, 115, 22, 0.1);
	}
}

.color-orange {
	color: #f97316;
}

.bg-slate-light {
	background: #f8fafc;

	@media (prefers-color-scheme: dark) {
		background: #0f172a;
	}
}

.color-slate {
	color: #64748b;
}

.hover-gray {
	background-color: #f8fafc !important;

	@media (prefers-color-scheme: dark) {
		background-color: #334155 !important;
	}
}

.mt-4 {
	margin-top: 32rpx;
}

.safe-area-bottom {
	height: 240rpx;
}

.material-icons {
	font-family: 'Material Symbols Outlined';
}

.btn-manage {
	background-color: $primary;
	color: #ffffff;
	font-size: 30rpx;
	font-weight: 600;
	width: 80%;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 32rpx;
	box-shadow: 0 8rpx 16rpx rgba($primary, 0.2);

	&::after {
		border: none;
	}
}

.btn-manage-hover {
	background-color: #3b76eb;
}
.scroll-body {
	height: calc(100vh - 120rpx);
}
</style>