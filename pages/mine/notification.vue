<template>
	<view class="container">
		<!-- Header -->
		<view class="header">
			<text class="title">系统通知</text>
			<text v-if="messages.length > 0" class="clear-btn" @tap="handleClearAll">清除全部</text>
		</view>

		<scroll-view scroll-y="true" class="list-wrapper" :style="{width: '100%'}">
			<!-- 空状态 -->
			<view v-if="messages.length === 0" class="empty-state">
				<image src="/static/empty_msg.png" mode="aspectFit" class="empty-img"></image>
				<text class="empty-text">暂无通知消息</text>
			</view>

			<!-- 按日期分组渲染 -->
			<view v-for="(group, gIndex) in groupedMessages" :key="group.label">
				<!-- 分组标题 -->
				<view class="section-header">
					<text class="section-label">{{ group.label }}</text>
					<text v-if="gIndex === 0" class="section-tag">LATEST</text>
				</view>

				<!-- 该组通知列表 -->
				<view
					v-for="item in group.items"
					:key="item.id"
					class="msg-card"
				>
					<!-- 左侧色条 -->
					<view class="card-accent" :class="'accent-' + item.type"></view>

					<!-- 图标区域 -->
					<view class="icon-wrap" :class="'icon-bg-' + item.type">
						<text class="material-symbols-outlined" :class="'icon-color-' + item.type">
							{{ getIcon(item.type) }}
						</text>
					</view>

					<!-- 内容区域 -->
					<view class="card-content">
						<view class="card-top">
							<text class="msg-title">{{ item.title }}</text>
							<view class="right-area">
								<text class="msg-time">{{ formatTimeOnly(item.time) }}</text>
								<view v-if="!item.isRead" class="unread-dot"></view>
							</view>
						</view>
						<text class="msg-body">{{ item.content }}</text>
					</view>
				</view>
			</view>

			<!-- 底部提示 -->
			<view class="footer-tip" v-if="messages.length > 0">
				<view class="footer-line"></view>
				<text class="footer-text">已显示全部通知</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			messages: []
		};
	},
	computed: {
		groupedMessages() {
			// 按日期分组：今天 / 昨天 / 具体日期
			const groups = [];
			const labelMap = {};

			this.messages.forEach(item => {
				const d = this.parseTime(item.time);
				const label = this.getDayLabel(d);
				if (!labelMap[label]) {
					labelMap[label] = [];
					groups.push({ label, items: labelMap[label], timestamp: d ? d.getTime() : 0 });
				}
				labelMap[label].push(item);
			});

			// 按时间降序排列分组（最新的在前）
			groups.sort((a, b) => b.timestamp - a.timestamp);
			return groups;
		}
	},
	onShow() {
		this.loadAndMarkRead();
	},
	methods: {
		loadAndMarkRead() {
			const list = uni.getStorageSync('SYSTEM_NOTIFICATIONS') || [];
			this.messages = list;
			if (list.some(m => !m.isRead)) {
				const newList = list.map(m => ({ ...m, isRead: true }));
				uni.setStorageSync('SYSTEM_NOTIFICATIONS', newList);
				uni.$emit('NEW_NOTIFICATION_RECEIVED', { hasUnread: false });
			}
		},
		handleClearAll() {
			uni.showModal({
				title: '提示',
				content: '确定要清空所有通知记录吗？',
				success: (res) => {
					if (res.confirm) {
						uni.removeStorageSync('SYSTEM_NOTIFICATIONS');
						this.messages = [];
						uni.$emit('NEW_NOTIFICATION_RECEIVED', { hasUnread: false });
					}
				}
			});
		},
		getIcon(type) {
			const map = {
				medicine_alarm:  'warning',
				medicine_update: 'monitoring',
				join_success:    'person_add',
				member_leave:    'person_remove',
			};
			return map[type] || 'notifications';
		},

		/**
		 * 解析 toLocaleString() 产生的中文时间字符串
		 * 例：'2026/3/21下午3:44:15'  '2026/3/21上午10:24:00'
		 */
		parseTime(timeStr) {
			if (!timeStr) return null;
			try {
				// 先尝试直接解析（国际化格式可能可以）
				const d = new Date(timeStr);
				if (!isNaN(d.getTime())) return d;

				// 处理中文 '上午/下午' 格式
				// 格式：YYYY/M/D上午/下午H:mm:ss 或 YYYY/M/D 上午/下午 H:mm:ss
				const reg = /(\d{4})\/(\d{1,2})\/(\d{1,2})\s*(上午|下午)\s*(\d{1,2}):(\d{2}):(\d{2})/;
				const m = timeStr.match(reg);
				if (m) {
					let hour = parseInt(m[5]);
					const isPM = m[4] === '下午';
					if (isPM && hour < 12) hour += 12;
					if (!isPM && hour === 12) hour = 0;
					return new Date(
						parseInt(m[1]),
						parseInt(m[2]) - 1,
						parseInt(m[3]),
						hour,
						parseInt(m[6]),
						parseInt(m[7])
					);
				}
				return null;
			} catch (e) {
				return null;
			}
		},

		// 返回分组标签：今天 / 昨天 / YYYY/M/D
		getDayLabel(date) {
			if (!date) return '更早';
			const now = new Date();
			const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
			const yesterdayStart = new Date(todayStart.getTime() - 86400000);
			if (date >= todayStart) return '今天';
			if (date >= yesterdayStart) return '昨天';
			return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`;
		},

		// 只返回时间部分：HH:MM，用于卡片右上角
		formatTimeOnly(timeStr) {
			const d = this.parseTime(timeStr);
			if (!d) return timeStr;
			const hh = String(d.getHours()).padStart(2, '0');
			const mm = String(d.getMinutes()).padStart(2, '0');
			return `${hh}:${mm}`;
		}
	}
};
</script>

<style lang="scss" scoped>
/* ===== 字体引入 ===== */
@font-face {
	font-family: 'MaterialIcons';
	src: url('~@/static/fonts/iconfont.ttf') format('truetype');
}

.material-symbols-outlined {
	font-family: 'MaterialIcons' !important;
	font-weight: normal;
	font-style: normal;
	font-size: 48rpx;
	line-height: 1;
	display: inline-block;
	white-space: nowrap;
	word-wrap: normal;
	direction: ltr;
	-webkit-font-smoothing: antialiased;
	text-rendering: optimizeLegibility;
	-moz-osx-font-smoothing: grayscale;
}

/* 图标颜色 */
.icon-color-medicine_alarm  { color: #b31b25; }
.icon-color-medicine_update { color: #0057bd; }
.icon-color-join_success    { color: #006a2d; }
.icon-color-member_leave    { color: #595c5e; }

/* ===== 整体容器 ===== */
.container {
	min-height: 100vh;
	background-color: #f5f7f9;
	display: flex;
	flex-direction: column;
}

/* ===== 顶部 Header ===== */
.header {
	position: sticky;
	top: 0;
	z-index: 50;
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 36rpx 48rpx;
	background: rgba(245, 247, 249, 0.92);
	backdrop-filter: blur(20rpx);
	box-shadow: 0 2rpx 20rpx rgba(0, 87, 189, 0.06);

	.title {
		font-size: 38rpx;
		font-weight: 800;
		color: #1a1c1e;
		letter-spacing: -0.5rpx;
	}

	.clear-btn {
		font-size: 30rpx;
		font-weight: 700;
		color: #0057bd;
		padding: 14rpx 32rpx;
		border-radius: 9999rpx;

		&:active {
			background: rgba(0, 87, 189, 0.08);
		}
	}
}

/* ===== 列表容器 ===== */
.list-wrapper {
	flex: 1;
	width: 100%;
	box-sizing: border-box;
	padding: 24rpx 40rpx 40rpx 40rpx;
	overflow-x: hidden;   /* 禁止横向滚动 */
}

/* ===== 分组标题 ===== */
.section-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 32rpx 8rpx 16rpx;
	width: 100%;
	box-sizing: border-box;

	.section-label {
		font-size: 34rpx;
		font-weight: 800;
		color: #2c2f31;
		letter-spacing: -0.3rpx;
	}

	.section-tag {
		font-size: 22rpx;
		font-weight: 600;
		color: #747779;
		letter-spacing: 3rpx;
	}
}

/* ===== 通知卡片 ===== */
.msg-card {
	position: relative;
	background: #ffffff;
	border-radius: 32rpx;
	margin-bottom: 20rpx;
	display: flex;
	flex-direction: row;
	align-items: center;
	overflow: hidden;
	box-shadow: 0 2rpx 20rpx rgba(0, 0, 0, 0.04);
	width: 100%;
	box-sizing: border-box;   /* 关键：让 padding/border 不撑破宽度 */

	&:active {
		box-shadow: 0 8rpx 40rpx rgba(0, 87, 189, 0.10);
	}
}

/* ===== 左侧色条 ===== */
.card-accent {
	position: absolute;
	left: 0;
	top: 0;
	bottom: 0;
	width: 10rpx;

	&.accent-medicine_alarm  { background: #fb5151; }
	&.accent-medicine_update { background: #6e9fff; }
	&.accent-join_success    { background: #6bff8f; }
	&.accent-member_leave    { background: #c4c6c8; }
}

/* ===== 图标圆形背景 ===== */
.icon-wrap {
	flex-shrink: 0;
	width: 96rpx;
	height: 96rpx;
	border-radius: 9999rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin: 32rpx 20rpx 32rpx 46rpx;

	&.icon-bg-medicine_alarm  { background: rgba(179, 27, 37, 0.08); }
	&.icon-bg-medicine_update { background: rgba(0, 87, 189, 0.08); }
	&.icon-bg-join_success    { background: rgba(0, 106, 45, 0.08); }
	&.icon-bg-member_leave    { background: rgba(89, 92, 94, 0.08); }
}

/* ===== 内容区域 ===== */
.card-content {
	flex: 1;
	min-width: 0;             /* 关键：防止 flex 子项撑破 */
	padding: 28rpx 32rpx 28rpx 0;
	box-sizing: border-box;

	.card-top {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 10rpx;
		width: 100%;

		.msg-title {
			font-size: 32rpx;
			font-weight: 700;
			color: #1a1c1e;
			flex: 1;
			min-width: 0;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
			margin-right: 16rpx;
		}

		.right-area {
			display: flex;
			align-items: center;
			gap: 10rpx;
			flex-shrink: 0;

			.msg-time {
				font-size: 24rpx;
				color: #747779;
			}
		}
	}

	.msg-body {
		font-size: 28rpx;
		color: #595c5e;
		line-height: 1.6;
	}
}

/* ===== 未读红点 ===== */
.unread-dot {
	width: 14rpx;
	height: 14rpx;
	background: #fb5151;
	border-radius: 50%;
	flex-shrink: 0;
}

/* ===== 空状态 ===== */
.empty-state {
	padding-top: 200rpx;
	display: flex;
	flex-direction: column;
	align-items: center;

	.empty-img {
		width: 240rpx;
		height: 240rpx;
		margin-bottom: 24rpx;
	}

	.empty-text {
		font-size: 28rpx;
		color: #abadaf;
	}
}

/* ===== 底部提示 ===== */
.footer-tip {
	padding: 60rpx 0 40rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 16rpx;

	.footer-line {
		width: 64rpx;
		height: 4rpx;
		background: #dfe3e6;
		border-radius: 9999rpx;
	}

	.footer-text {
		font-size: 24rpx;
		color: #abadaf;
		letter-spacing: 1rpx;
	}
}
</style>