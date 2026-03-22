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

			<!-- ── 无任务状态 ── -->
			<view v-if="taskList.length === 0" class="empty-card">
				<view class="ec-blob1"></view>
				<view class="ec-blob2"></view>
				<view class="ec-center">
					<view class="ec-graphic">
						<view class="ec-outer" :style="blobOuterStyle"></view>
						<view class="ec-inner" :style="blobInnerStyle"></view>
						<view class="ec-icon-wrap">
							<view class="ec-dots">
								<view class="ec-dot-g"></view>
								<view class="ec-dot-b"></view>
							</view>
							<text class="ec-spa-icon">🌿</text>
						</view>
					</view>
					<text class="ec-title">今日没有服药任务</text>
					<text class="ec-sub">享受健康生活的一天吧！</text>
				</view>
			</view>

			<!-- ── 有任务状态 ── -->
			<view v-else class="task-area">
				<!-- 叠影层：仅任务 > 1 时显示 -->
				<template v-if="taskList.length > 1">
					<view class="stack-3"></view>
					<view class="stack-2"></view>
				</template>

				<!-- 只展示第一条任务，复用原有 med-card 样式 -->
				<view class="med-card" :class="getStatusClass(taskList[0].status)">
					<view class="med-info-left">
						<view class="time-box">
							<text class="time-text">{{ taskList[0].timePoint }}</text>
							<view class="status-dot"></view>
						</view>
						<view class="name-box">
							<text class="med-name">{{ taskList[0].medicineName }}</text>
							<text class="med-dosage">{{ taskList[0].dosage }}</text>
						</view>
					</view>
					<view class="med-action">
						<view v-if="taskList[0].status === 1" class="status-done-text">
							<text class="iconfont icon-check"></text>
							<text>已服</text>
						</view>
						<button v-else-if="taskList[0].status === 0" class="take-btn"
							@tap="handleTaskClick(taskList[0], 0)">服用</button>
						<text v-else-if="taskList[0].status === 2" class="status-missed-text">！漏服</text>
					</view>
				</view>

				<text class="task-hint">在用药提醒中查看所有任务</text>
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
			hasNotification: false,
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
			blobOuterStyle: {},
			blobInnerStyle: {},
			_blobTimer: null,
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
		this._startBlobAnimation();
	},
	
	beforeDestroy() {
	  if (this._blobTimer) clearInterval(this._blobTimer); // 新增
	},
	
	onShow() {
	    // 每次回到首页，检查一次是否有未读消息
	    this.checkUnreadStatus();
	},
	
	onLoad() {
	    // 监听 WS 接收到新消息的事件
	    uni.$on('NEW_NOTIFICATION_RECEIVED', (data) => {
	        this.hasNotification = data.hasUnread;
	    });
	},
	
	onUnload() {
	    uni.$off('NEW_NOTIFICATION_RECEIVED');
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
			const id = uni.getStorageSync("userId") || 1;
			// 用 userId 做简单哈希，映射到 1-100
			const index = (Math.abs(Number(id)) % 100) + 1;
			return `http://8.148.94.242/avatar/file/avatar_${index}.svg`;
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
		toMedicationList() { uni.navigateTo({ url: "/pages/medication/list" }); },
		toHealthDetail() { uni.navigateTo({ url: "/pages/health/detail" }); },

		_startBlobAnimation() {
		  const outerFrames = [
		    'linear-gradient(135deg, rgba(96,165,250,0.2), rgba(52,211,153,0.2))',
		  ]
		  const borderFrames = [
		    { outer: '60% 40% 30% 70% / 60% 30% 70% 40%', inner: '50% 60% 40% 50% / 40% 50% 60% 50%' },
		    { outer: '30% 60% 70% 40% / 50% 60% 30% 60%', inner: '60% 40% 50% 60% / 60% 40% 50% 40%' },
		    { outer: '50% 50% 40% 60% / 30% 70% 50% 50%', inner: '40% 60% 60% 40% / 50% 40% 60% 60%' },
		    { outer: '70% 30% 50% 50% / 60% 40% 60% 40%', inner: '55% 45% 45% 55% / 45% 55% 45% 55%' },
		  ]
		  let i = 0
		  const next = () => {
		    const f = borderFrames[i % borderFrames.length]
		    this.blobOuterStyle = {
		      borderRadius: f.outer,
		      transition: 'border-radius 2.5s ease-in-out',
		    }
		    this.blobInnerStyle = {
		      borderRadius: f.inner,
		      transition: 'border-radius 2s ease-in-out',
		    }
		    i++
		  }
		  next()
		  this._blobTimer = setInterval(next, 2600)
		},
		
		checkUnreadStatus() {
		        // 从本地存储读取，判断是否有没有读过的消息
		        const list = uni.getStorageSync('SYSTEM_NOTIFICATIONS') || [];
		        this.hasNotification = list.some(item => item.isRead === false);
		},
		
		toNotification() {
			// 点击进入通知页面
			uni.navigateTo({
				url: '/pages/mine/notification', // 你的通知页面路径
				success: () => {
					// 只要点进去了，首页红点可以先消失（或者由通知页面处理）
					this.hasNotification = false;
				}
			});
		},

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
				// uni.showToast({ title: "操作失败", icon: "none" });
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

/* ────────────────────────────────────────
   新增：无任务空状态
──────────────────────────────────────── */
.empty-card {
	position: relative;
	min-height: 580rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: #ffffff;
	border-radius: 80rpx;
	overflow: hidden;
	border: 1rpx solid #f1f5f9;
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.ec-blob1 {
	position: absolute;
	top: -80rpx;
	right: -80rpx;
	width: 380rpx;
	height: 380rpx;
	background: rgba(191, 219, 254, 0.4);
	border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
	filter: blur(60rpx);
}

.ec-blob2 {
	position: absolute;
	bottom: -96rpx;
	left: -64rpx;
	width: 440rpx;
	height: 440rpx;
	background: rgba(167, 243, 208, 0.3);
	border-radius: 30% 60% 70% 40% / 50% 60% 30% 60%;
	filter: blur(60rpx);
}

.ec-center {
	display: flex;
	flex-direction: column;
	align-items: center;
	position: relative;
	z-index: 10;
}

.ec-graphic {
	position: relative;
	width: 320rpx;
	height: 320rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 48rpx;
}

.ec-outer {
	position: absolute;
	inset: 0;
	background: linear-gradient(135deg, rgba(96, 165, 250, 0.2), rgba(52, 211, 153, 0.2));
	border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
}

.ec-inner {
	position: absolute;
	top: 32rpx;
	left: 32rpx;
	right: 32rpx;
	bottom: 32rpx;
	background: linear-gradient(225deg, rgba(255, 255, 255, 0.85), rgba(239, 246, 255, 0.5));
	border-radius: 50%;
	box-shadow: inset 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.ec-icon-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 12rpx;
	position: relative;
	z-index: 1;
}

.ec-dots {
	display: flex;
	flex-direction: row;
	gap: 12rpx;
}

.ec-dot-g {
	width: 20rpx;
	height: 20rpx;
	border-radius: 50%;
	background: #34d399;
	box-shadow: 0 0 24rpx rgba(52, 211, 153, 0.5);
}

.ec-dot-b {
	width: 20rpx;
	height: 20rpx;
	border-radius: 50%;
	background: #60a5fa;
	box-shadow: 0 0 24rpx rgba(96, 165, 250, 0.5);
}

.ec-spa-icon {
	font-size: 88rpx;
	line-height: 1;
}

.ec-title {
	font-size: 36rpx;
	font-weight: 700;
	color: #1e293b;
}

.ec-sub {
	font-size: 28rpx;
	color: #94a3b8;
	font-weight: 500;
	margin-top: 16rpx;
}

/* ────────────────────────────────────────
   新增：有任务堆叠区域
──────────────────────────────────────── */
.task-area {
	position: relative;
	padding-bottom: 56rpx;
}

/* 第三层（最底，最窄最透明） */
.stack-3 {
	position: absolute;
	left: 88rpx;
	right: 88rpx;
	bottom: 28rpx;
	height: 100rpx;
	background: rgba(255, 255, 255, 0.45);
	border-radius: 40rpx;
	border: 1rpx solid rgba(226, 232, 240, 0.4);
	box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.03);
}

/* 第二层（中间） */
.stack-2 {
	position: absolute;
	left: 44rpx;
	right: 44rpx;
	bottom: 42rpx;
	height: 100rpx;
	background: rgba(255, 255, 255, 0.72);
	border-radius: 40rpx;
	border: 1rpx solid rgba(226, 232, 240, 0.75);
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.task-hint {
	display: block;
	text-align: center;
	font-size: 24rpx;
	color: #94a3b8;
	font-weight: 500;
	letter-spacing: 0.04em;
	margin-top: 16rpx;
}

/* ────────────────────────────────────────
   原有：服药卡片样式（完全不变，仅新增
   position/z-index/margin-bottom 覆盖）
──────────────────────────────────────── */
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

	/* 新增：确保主卡片在叠影层之上 */
	position: relative;
	z-index: 2;

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
	background-color: #0f172a;

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
		}

		/* 待服药状态下，按钮如果需要暗化可以加在这里 */
		&.is-pending {
			border-color: rgba(99, 102, 241, 0.5);
		}
	}

	/* 4. 空状态暗色适配（新增） */
	.empty-card {
		background: #1e293b;
		border-color: #334155;

		.ec-title {
			color: #ffffff;
		}

		.ec-inner {
			background: linear-gradient(225deg, rgba(30, 41, 59, 0.85), rgba(15, 23, 42, 0.5));
		}
	}

	/* 5. 堆叠层暗色适配（新增） */
	.stack-3 {
		background: rgba(30, 41, 59, 0.45);
		border-color: rgba(51, 65, 85, 0.4);
	}

	.stack-2 {
		background: rgba(30, 41, 59, 0.72);
		border-color: rgba(51, 65, 85, 0.75);
	}
}
</style>