<template>
	<scroll-view class="container" scroll-y :class="{ 'dark-mode': isDarkMode }" :style="{ '--base-font': globalFontSize + 'px' }">

		<view class="padding"></view>
		<view class="header">
			<view class="user-section">
				<view class="user-info" @tap="toProfile">
					<view class="avatar-wrapper">
						<image :src="getAvatar()" mode="aspectFill" class="avatar"></image>
						<view v-if="userInfo.hasNew" class="status-online"></view>
					</view>
					<view class="welcome-text">
						<text class="greet">你好，</text>
						<text class="name">{{ userInfo.name }}</text>
					</view>
				</view>
				<view class="notification-btn" @tap="toNotification">
					<view class="icon" style="background-image: url('static/Home/bell.svg');"></view>
					<view v-if="hasNotification" class="dot-red"></view>
				</view>
			</view>

			<view class="date-section">
				<view class="date-left">
					<text class="day-num">{{ currentDay }}</text>
					<view class="month-year">
						<text>{{ currentMonth }}月</text>
						<text>{{ currentYear }}</text>
					</view>
				</view>
				<view class="week-tag">
					<text>{{ currentWeek }}</text>
				</view>
			</view>
		</view>

		<view class="section-box">
			<view class="section-title">
				<text class="title-text">今日服药任务</text>
				<view class="more-link" @tap="toMedicationList">
					<text style="font-size: calc(var(--base-font) + 2rpx);" @click="uni.navigateTo({ url: '/pages/reminder/Reminder' })">任务中心</text>
					<text class="iconfont icon-arrow-right"></text>
				</view>
			</view>

			<view class="med-list">
				<view v-for="(item, index) in taskList" :key="item.id" class="med-card"
					:class="getStatusClass(item.status)">
					<view class="med-info-left">
						<view class="time-box">
							<text class="time-text">{{ item.timePoint }}</text>
							<view class="status-dot"></view>
						</view>
						<view class="name-box">
							<text class="med-name">{{ item.medicineName }}</text>
							<text class="med-dosage">{{ item.dosage }}</text>
						</view>
					</view>

					<view class="med-action">
						<view v-if="item.status === 1" class="status-done-text">
							<text class="iconfont icon-check"></text>
							<text>已服</text>
						</view>
						<button v-else-if="item.status === 0" class="take-btn"
							@tap="handleTaskClick(item, index)">服用</button>
						<text v-else-if="item.status === 2" class="status-missed-text">！漏服</text>
					</view>
				</view>
			</view>
		</view>

		<quick-tools />
		<view class="bottom-safe-spacer"></view>

	</scroll-view>
</template>

<script>
import quickTools from '../../components/quick-tools.vue';
import reminderApi from "../../api/reminder.js";
// 这里直接使用你提供的 script 逻辑
export default {
	components: { quickTools },
	data() {
		return {
			globalFontSize: 16, // 默认基准字体大小 16px
			isDarkMode: false,
			refreshing: false,
			userInfo: { name: "小明", avatar: "static/avatars/avatar1.svg", hasNew: true },
			hasNotification: true,
			medicationList: [
				{ id: 1, medicineName: "阿司匹林", timePoint: "10:00", dosage: "1片", status: 1 },
				{ id: 2, medicineName: "降压药", timePoint: "14:00", dosage: "1片", status: 0 },
				{ id: 3, medicineName: "维C", timePoint: "08:00", dosage: "2粒", status: 2 },
			],
			functions: [
				{ id: 1, name: "拍照识药", icon: "/static/Home/camera.svg", color: "#06b6d4" },
				{ id: 2, name: "就医清单", icon: "/static/Home/medical-list.svg", color: "#6366f1" },
				{ id: 3, name: "家庭管理", icon: "/static/Home/family.svg", color: "#10b981" },
				{ id: 4, name: "AI 咨询", icon: "/static/Home/ai-assistant.svg", color: "#0ea5e9" },
			],
			healthData: { steps: 6234, heartRate: 72 },
			taskList: [],
		};
	},
	computed: {
		currentYear() { return new Date().getFullYear(); },
		currentMonth() { return new Date().getMonth() + 1; },
		currentDay() { return new Date().getDate(); },
		currentWeek() { return "星期" + ["日", "一", "二", "三", "四", "五", "六"][new Date().getDay()]; },
	},
	mounted() {
		console.log("Home onShow");
		this.applyGlobalSettings();
		this.fetchTasks();
		this.initData();
	},
	methods: {
		// 读取全局设置
		applyGlobalSettings() {
			const savedFont = uni.getStorageSync('app_font_size');
			console.log('读取到的全局字体大小:', savedFont); // 调试输出，查看读取到的值
			if (savedFont) {
				this.globalFontSize = savedFont;
			}
			const savedTheme = uni.getStorageSync('app_theme');
			this.isDarkMode = (savedTheme === 'dark');
		},
		initData() {
			const username = uni.getStorageSync("username") || "用户";
			this.userInfo.name = username;
		},
		getAvatar() {
			const userId = uni.getStorageSync("userId") || "defaultUser";
			return `https://api.dicebear.com/7.x/adventurer/svg?seed=${userId}`;
		},
		getStatusClass(status) {
			const map = { 0: "is-pending", 1: "is-done", 2: "is-missed" };
			return map[status] || "";
		},
		async handleTakeMedicine(item) {
			uni.showLoading({ title: "确认中..." });
			setTimeout(() => {
				item.status = 1;
				uni.hideLoading();
				uni.showToast({ title: "已记录服用" });
			}, 500);
		},
		onRefresh() {
			this.refreshing = true;
			setTimeout(() => {
				this.refreshing = false;
				uni.showToast({ title: "已同步最新数据", icon: "none" });
			}, 800);
		},
		handleFunction(funcId) {
			const routes = {
				1: "/pages/scan/DrugScan",
				2: "/pages/medical/Prepare",
				3: "/pages/family/index",
				4: "/pages/ai/Assistant",
			};
			if (routes[funcId]) uni.navigateTo({ url: routes[funcId] });
		},
		toProfile() { uni.navigateTo({ url: "/pages/profile/profile" }); },
		toNotification() { uni.navigateTo({ url: "/pages/notification/notification" }); },
		toMedicationList() { uni.navigateTo({ url: "/pages/medication/list" }); },
		toHealthDetail() { uni.navigateTo({ url: "/pages/health/detail" }); },

		async fetchTasks() {
			try {
				const res = await reminderApi.getTodayTasks(this.userId);
				console.log("fetchTasks res: ", res);
				if (res && res.code === 200) {
					// 假设 api 返回结构里 data 才是数组，或者根据你的封装 res 直接是数组
					const list = res.data || [];
					this.taskList = list.sort((a, b) =>
						a.timePoint.localeCompare(b.timePoint),
					);
				} else if (Array.isArray(res)) {
					// 兼容你的封装直接返回 data 的情况
					this.taskList = res.sort((a, b) =>
						a.timePoint.localeCompare(b.timePoint),
					);
				}
			} catch (e) {
				console.error("获取任务失败", e);
			}
		},
		async handleTaskClick(task, index) {
			if (task.status === 1 || task.status === 2) return;

			try {
				const res = await reminderApi.updateTaskStatus(task.id, this.userId, 1);
				this.taskList[index].status = 1;
				this.taskList[index].operateTime = new Date().toISOString();
				uni.showToast({ title: "已服药", icon: "success" });
			} catch (e) {
				uni.showToast({ title: "操作失败", icon: "none" });
			}
		},
	},
};
</script>

<style lang="scss" scoped>
.container {
	height: 100vh;
	background-color: #f8fafc;

	@media (prefers-color-scheme: dark) {
		background-color: #0f172a;
	}
}

.padding {
	height: 64rpx;
}

.header {
	padding: 60rpx 40rpx 30rpx;

	.user-section {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 40rpx;

		.user-info {
			display: flex;
			align-items: center;
			gap: 20rpx;

			.avatar-wrapper {
				position: relative;

				.avatar {
					width: 110rpx;
					height: 110rpx;
					border-radius: 50%;
					border: 4rpx solid white;
					box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.05);
				}

				.status-online {
					position: absolute;
					bottom: 4rpx;
					right: 4rpx;
					width: 28rpx;
					height: 28rpx;
					background: #10b981;
					border: 4rpx solid white;
					border-radius: 50%;
				}
			}

			.welcome-text {
				.greet {
					font-size: calc(var(--base-font) + 8rpx);
					color: #64748b;
					display: block;
				}

				.name {
					font-size: calc(var(--base-font) + 8rpx);
					font-weight: bold;
					color: #1e293b;

					@media (prefers-color-scheme: dark) {
						color: #fff;
					}
				}
			}
		}

		.notification-btn {
			width: 100rpx;
			height: 100rpx;
			background: white;
			border-radius: 30rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			position: relative;

			@media (prefers-color-scheme: dark) {
				background: #1e293b;
			}

			.icon-bell {
				font-size: calc(var(--base-font) + 8rpx);
				color: #64748b;
			}

			.dot-red {
				position: absolute;
				top: 28rpx;
				right: 28rpx;
				width: 14rpx;
				height: 14rpx;
				background: #ef4444;
				border-radius: 50%;
			}
		}
	}

	.date-section {
		display: flex;
		justify-content: space-between;
		align-items: flex-end;

		.date-left {
			display: flex;
			align-items: center;
			gap: 15rpx;

			.day-num {
				font-size: calc(var(--base-font) + 32rpx);
				font-weight: 900;
				color: #6366f1;
				line-height: 1;
			}

			.month-year {
				font-size: calc(var(--base-font) + 8rpx);
				color: #64748b;
				font-weight: 500;
			}
		}

		.week-tag {
			padding: 12rpx 30rpx;
			background: #eef2ff;
			color: #6366f1;
			border-radius: 40rpx;
			font-size: calc(var(--base-font) + 4rpx);
			font-weight: 600;

			@media (prefers-color-scheme: dark) {
				background: rgba(99, 102, 241, 0.2);
			}
		}
	}
}

.section-box {
	padding: 0 40rpx;
	margin-top: 50rpx;

	.section-title {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 24rpx;

		.title-text {
			font-size: calc(var(--base-font) + 1rpx);
			font-weight: bold;
			color: #1e293b;

			@media (prefers-color-scheme: dark) {
				color: #fff;
			}
		}

		.more-link {
			font-size: calc(var(--base-font) + 8rpx);
			color: #94a3b8;
			display: flex;
			align-items: center;
		}
	}
}

/* 服药卡片样式 */
.med-card {
	background: white;
	padding: 36rpx;
	border-radius: 40rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 24rpx;
	border: 2rpx solid #f1f5f9;
	box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.02);

	@media (prefers-color-scheme: dark) {
		background: #1e293b;
		border-color: #334155;
	}

	.med-info-left {
		display: flex;
		gap: 30rpx;
		align-items: center;

		.time-box {
			text-align: center;

			.time-text {
				font-size: calc(var(--base-font) + 1rpx);
				font-weight: bold;
				color: #475569;
				display: block;

				@media (prefers-color-scheme: dark) {
					color: #e2e8f0;
				}
			}

			.status-dot {
				width: 14rpx;
				height: 14rpx;
				border-radius: 50%;
				display: inline-block;
			}
		}

		.name-box {
			.med-name {
				font-size: calc(var(--base-font) + 1rpx);
				font-weight: 600;
				color: #1e293b;
				display: block;

				@media (prefers-color-scheme: dark) {
					color: #fff;
				}
			}

			.med-dosage {
				font-size: calc(var(--base-font) + 1rpx);
				color: #94a3b8;
			}
		}
	}

	/* 不同状态色彩 */
	&.is-done {
		.status-dot {
			background: #10b981;
		}

		.status-done-text {
			color: #10b981;
			font-weight: 500;
			font-size: calc(var(--base-font) + 1rpx);
		}
	}

	&.is-pending {
		border: 2rpx solid rgba(99, 102, 241, 0.3);

		.status-dot {
			background: #6366f1;
		}

		.take-btn {
			background: #6366f1;
			color: white;
			padding: 8rpx 24rpx;
			border-radius: 20rpx;
			font-size: calc(var(--base-font) + 1rpx);
			font-weight: 600;
		}
	}

	&.is-missed {
		background: #fff1f2;
		border-color: #ffe4e6;

		@media (prefers-color-scheme: dark) {
			background: rgba(225, 29, 72, 0.1);
			border-color: rgba(225, 29, 72, 0.2);
		}

		.time-text,
		.status-missed-text {
			color: #e11d48;
		}

		.status-dot {
			background: #e11d48;
		}

		.status-missed-text {
			font-weight: bold;
			font-size: calc(var(--base-font) + 1rpx);
		}
	}
}

/* 快捷工具网格 */
.tool-grid {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 20rpx;

	.tool-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 16rpx;

		.icon-bg {
			width: 120rpx;
			height: 120rpx;
			border-radius: 36rpx;
			display: flex;
			align-items: center;
			justify-content: center;
		}

		.tool-name {
			font-size: calc(var(--base-font) + 1rpx);
			color: #64748b;
			font-weight: 500;
		}

		&:active {
			transform: scale(0.92);
		}
	}
}

/* 图标库占位 */
.iconfont {
	font-family: "Material Symbols Outlined";
	font-size: calc(var(--base-font) + 20rpx);
}

.bottom-safe-spacer {
	/* 基础高度：需大于你的 TabBar 高度 */
	height: 240rpx;
	width: 100%;
	background-color: transparent;

	/* 适配 iPhone X 及以上的底部小白条安全区 */
	padding-bottom: constant(safe-area-inset-bottom);
	padding-bottom: env(safe-area-inset-bottom);
}

.bottom-safe-spacer {
	height: 200rpx;
}

/* ====================================================
   暗黑模式全局覆盖样式 (通过 .dark-mode 类触发)
==================================================== */
.dark-mode.container {
    background-color: #0f172a; /* 页面深色底色 */

    /* 1. 头部区域 */
    .header {
        .user-info .welcome-text .name {
            color: #ffffff;
        }

        .notification-btn {
            background: #1e293b;
        }

        .date-section .week-tag {
            background: rgba(99, 102, 241, 0.2);
        }
    }

    /* 2. 任务标题区域 */
    .section-box {
        .section-title .title-text {
            color: #ffffff;
        }
    }

    /* 3. 服药卡片基础样式 */
    .med-card {
        background: #1e293b;
        border-color: #334155;

        .med-info-left {
            .time-box .time-text {
                color: #e2e8f0;
            }

            .name-box .med-name {
                color: #ffffff;
            }
        }

        /* 漏服状态下的卡片深色适配 */
        &.is-missed {
            background: rgba(225, 29, 72, 0.1);
            border-color: rgba(225, 29, 72, 0.2);
            /* 注意：这里的红色文字和红点因为对比度尚可，可以保留原有的 #e11d48，或者按需微调 */
        }
        
        /* 待服药状态下，按钮如果需要暗化可以加在这里 */
        &.is-pending {
            border-color: rgba(99, 102, 241, 0.5);
        }
    }
}
</style>