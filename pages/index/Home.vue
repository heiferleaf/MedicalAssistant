<template>
	<view class="container">
		<scroll-view scroll-y class="scroll-content" @refresherrefresh="onRefresh" :refresher-enabled="true" :refresher-triggered="refreshing">
			
			<view class="header">
				<view class="user-section">
					<view class="user-info" @tap="toProfile">
						<view class="avatar-wrapper">
							<image :src="userInfo.avatar || 'https://lh3.googleusercontent.com/aida-public/AB6AXuCZpfuaPiebYwNl3z3EeTZ6AcZIjM8Mo5ltsxFNMsKzqf_IU2qSOPT6B2L4oAEAHzD5_O8XQiSSXZekjuORU6gUC7Ui_Qe7yziFYuGB_yeo14vMl6KXang1WNRUO3OFgNoFekIaF55NRSoLW44ky7KX0vmAACY-ajMihfzGHeLUo4MIxU9JgQHcwCt-U-gDqxONsk4x6vQIvvMISX74KYV62XNy0KW2drMweZ7DHTmi6vtOh3LgDsci2ckwRDn724jyxP3cY2pYkZ8'" mode="aspectFill" class="avatar"></image>
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
						<text>任务中心</text>
						<text class="iconfont icon-arrow-right"></text>
					</view>
				</view>

				<view class="med-list">
					<view 
						v-for="item in medicationList" 
						:key="item.id" 
						class="med-card" 
						:class="getStatusClass(item.status)"
					>
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
							<button v-else-if="item.status === 0" class="take-btn" @tap="handleTakeMedicine(item)">服用</button>
							<text v-else-if="item.status === 2" class="status-missed-text">！漏服</text>
						</view>
					</view>
				</view>
			</view>

			<view class="section-box">
				<view class="section-title">
					<text class="title-text">快捷工具</text>
				</view>
				<view class="tool-grid">
					<view 
						v-for="func in functions" 
						:key="func.id" 
						class="tool-item" 
						@tap="handleFunction(func.id)"
					>
						<view class="icon-bg" :style="{ backgroundColor: func.color + '20' }">
							<image 
              :src="func.icon" 
              mode="aspectFit" 
              class="icon"
              style="height: 48rpx;width: 48rpx;"
              />
						</view>
						<text class="tool-name">{{ func.name }}</text>
					</view>
				</view>
			</view>
      <view style="height: 100rpx;width:100%;background: transparent;"></view>

		</scroll-view>
	</view>
</template>

<script>
// 这里直接使用你提供的 script 逻辑
export default {
	data() {
		return {
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
		};
	},
	computed: {
		currentYear() { return new Date().getFullYear(); },
		currentMonth() { return new Date().getMonth() + 1; },
		currentDay() { return new Date().getDate(); },
		currentWeek() { return "星期" + ["日", "一", "二", "三", "四", "五", "六"][new Date().getDay()]; },
	},
	methods: {
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
				3: "/pages/family/manage",
				4: "/pages/ai/Assistant",
			};
			if (routes[funcId]) uni.navigateTo({ url: routes[funcId] });
		},
		toProfile() { uni.navigateTo({ url: "/pages/profile/profile" }); },
		toNotification() { uni.navigateTo({ url: "/pages/notification/notification" }); },
		toMedicationList() { uni.navigateTo({ url: "/pages/medication/list" }); },
		toHealthDetail() { uni.navigateTo({ url: "/pages/health/detail" }); },
	},
};
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #f8fafc;
  
  /* 原有的 180rpx 避开悬浮栏 + 额外的 100rpx 留白 */
  padding-bottom: 280rpx; 
  
  /* 如果你的 App 需要适配 iPhone 底部安全区（刘海屏），建议这样写： */
  padding-bottom: calc(280rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(280rpx + env(safe-area-inset-bottom));

  @media (prefers-color-scheme: dark) { background-color: #0f172a; }
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
				.avatar { width: 110rpx; height: 110rpx; border-radius: 50%; border: 4rpx solid white; box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.05); }
				.status-online { position: absolute; bottom: 4rpx; right: 4rpx; width: 28rpx; height: 28rpx; background: #10b981; border: 4rpx solid white; border-radius: 50%; }
			}
			
			.welcome-text {
				.greet { font-size: 26rpx; color: #64748b; display: block; }
				.name { font-size: 38rpx; font-weight: bold; color: #1e293b; @media (prefers-color-scheme: dark) { color: #fff; } }
			}
		}

		.notification-btn {
			width: 100rpx; height: 100rpx; background: white; border-radius: 30rpx; display: flex; align-items: center; justify-content: center; position: relative;
			@media (prefers-color-scheme: dark) { background: #1e293b; }
			.icon-bell { font-size: 48rpx; color: #64748b; }
			.dot-red { position: absolute; top: 28rpx; right: 28rpx; width: 14rpx; height: 14rpx; background: #ef4444; border-radius: 50%; }
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
			.day-num { font-size: 100rpx; font-weight: 900; color: #6366f1; line-height: 1; }
			.month-year { font-size: 28rpx; color: #64748b; font-weight: 500; }
		}
		
		.week-tag {
			padding: 12rpx 30rpx; background: #eef2ff; color: #6366f1; border-radius: 40rpx; font-size: 26rpx; font-weight: 600;
			@media (prefers-color-scheme: dark) { background: rgba(99, 102, 241, 0.2); }
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
		.title-text { font-size: 34rpx; font-weight: bold; color: #1e293b; @media (prefers-color-scheme: dark) { color: #fff; } }
		.more-link { font-size: 26rpx; color: #94a3b8; display: flex; align-items: center; }
	}
}

/* 服药卡片样式 */
.med-card {
	background: white; padding: 36rpx; border-radius: 40rpx; display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx;
	border: 2rpx solid #f1f5f9; box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.02);
	@media (prefers-color-scheme: dark) { background: #1e293b; border-color: #334155; }
	
	.med-info-left {
		display: flex; gap: 30rpx; align-items: center;
		.time-box {
			text-align: center;
			.time-text { font-size: 32rpx; font-weight: bold; color: #475569; display: block; @media (prefers-color-scheme: dark) { color: #e2e8f0; } }
			.status-dot { width: 14rpx; height: 14rpx; border-radius: 50%; display: inline-block; }
		}
		.name-box {
			.med-name { font-size: 32rpx; font-weight: 600; color: #1e293b; display: block; @media (prefers-color-scheme: dark) { color: #fff; } }
			.med-dosage { font-size: 26rpx; color: #94a3b8; }
		}
	}

	/* 不同状态色彩 */
	&.is-done {
		.status-dot { background: #10b981; }
		.status-done-text { color: #10b981; font-weight: 500; font-size: 28rpx; }
	}
	&.is-pending {
		border: 2rpx solid rgba(99, 102, 241, 0.3);
		.status-dot { background: #6366f1; }
		.take-btn { background: #6366f1; color: white; padding: 12rpx 36rpx; border-radius: 20rpx; font-size: 26rpx; font-weight: 600; }
	}
	&.is-missed {
		background: #fff1f2; border-color: #ffe4e6;
		@media (prefers-color-scheme: dark) { background: rgba(225, 29, 72, 0.1); border-color: rgba(225, 29, 72, 0.2); }
		.time-text, .status-missed-text { color: #e11d48; }
		.status-dot { background: #e11d48; }
		.status-missed-text { font-weight: bold; font-size: 28rpx; }
	}
}

/* 快捷工具网格 */
.tool-grid {
	display: grid; grid-template-columns: repeat(4, 1fr); gap: 20rpx;
	.tool-item {
		display: flex; flex-direction: column; align-items: center; gap: 16rpx;
		.icon-bg { width: 120rpx; height: 120rpx; border-radius: 36rpx; display: flex; align-items: center; justify-content: center; }
		.tool-name { font-size: 24rpx; color: #64748b; font-weight: 500; }
		&:active { transform: scale(0.92); }
	}
}

/* 图标库占位 */
.iconfont { font-family: "Material Symbols Outlined"; font-size: 48rpx; }
</style>