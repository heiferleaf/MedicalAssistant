<template>
	<view class="container">
		<view class="header">
			<text class="title">系统通知</text>
			<text v-if="messages.length > 0" class="clear-btn" @tap="handleClearAll">清空全部</text>
		</view>

		<scroll-view scroll-y="true" class="list-wrapper">
			<view v-if="messages.length === 0" class="empty-state">
				<image src="/static/empty_msg.png" mode="aspectFit" class="empty-img"></image>
				<text>暂无通知消息</text>
			</view>

			<view 
				v-for="(item, index) in messages" 
				:key="item.id" 
				class="msg-card"
				:class="{ 'is-unread': !item.isRead }"
			>
				<view class="card-line" :class="item.type"></view>
				<view class="card-content">
					<view class="card-top">
						<text class="msg-title">{{ item.title }}</text>
						<text class="msg-time">{{ item.time }}</text>
					</view>
					<view class="msg-body">{{ item.content }}</view>
				</view>
			</view>
			
			<view class="footer-tip" v-if="messages.length > 0">仅保留最近 100 条通知</view>
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
	onShow() {
		this.loadAndMarkRead();
	},
	methods: {
		// 加载消息并全部标记为已读
		loadAndMarkRead() {
			const list = uni.getStorageSync('SYSTEM_NOTIFICATIONS') || [];
			this.messages = list;
			
			// 如果有未读消息，更新状态
			if (list.some(m => !m.isRead)) {
				const newList = list.map(m => ({ ...m, isRead: true }));
				uni.setStorageSync('SYSTEM_NOTIFICATIONS', newList);
				// 通知首页红点可以消失了
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
		}
	}
};
</script>

<style lang="scss" scoped>
.container {
	min-height: 100vh;
	background-color: #f8f9fa;
	display: flex;
	flex-direction: column;
}

.header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 30rpx 40rpx;
	background: #fff;
	.title { font-size: 36rpx; font-weight: bold; color: #333; }
	.clear-btn { font-size: 26rpx; color: #999; }
}

.list-wrapper {
	flex: 1;
	padding: 20rpx;
}

.msg-card {
	background: #fff;
	border-radius: 16rpx;
	margin-bottom: 20rpx;
	display: flex;
	overflow: hidden;
	box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.03);
	position: relative;
	
	&.is-unread::after {
		content: "";
		position: absolute;
		top: 20rpx;
		right: 20rpx;
		width: 12rpx;
		height: 12rpx;
		background: #ff4d4f;
		border-radius: 50%;
	}
}

.card-line {
	width: 8rpx;
	background: #2d8cf0; // 默认蓝色
	&.medicine_alarm { background: #ff4d4f; } // 告警红色
	&.medicine_update { background: #52c41a; } // 更新绿色
}

.card-content {
	flex: 1;
	padding: 24rpx;
	.card-top {
		display: flex;
		justify-content: space-between;
		margin-bottom: 12rpx;
		.msg-title { font-size: 30rpx; font-weight: 600; color: #333; }
		.msg-time { font-size: 24rpx; color: #999; }
	}
	.msg-body {
		font-size: 28rpx;
		color: #666;
		line-height: 1.4;
	}
}

.empty-state {
	padding-top: 200rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	color: #999;
	.empty-img { width: 240rpx; height: 240rpx; margin-bottom: 20rpx; }
}

.footer-tip {
	text-align: center;
	font-size: 24rpx;
	color: #ccc;
	padding: 40rpx 0;
}
</style>