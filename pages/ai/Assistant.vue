<template>
	<view class="chat-container">
		<view class="status-bar"></view>

		<view class="header">
			<view class="header-left">
				<view class="ai-avatar">AI</view>
				<view class="header-info">
					<text class="header-title">智愈助手</text>
					<view class="online-status">
						<view class="dot"></view>
						<text class="status-text">在线 | 为您的健康护航</text>
					</view>
				</view>
			</view>
		</view>

		<scroll-view scroll-y class="chat-main" :scroll-into-view="lastMsgId" scroll-with-animation>
			<view class="chat-content">
				
				<view class="ai-msg-group">
					<view class="chat-bubble-ai card-shadow">
						<text class="msg-text-bold">早上好！今天您有 <text class="highlight">3种</text> 药物需要服用。 💊</text>
						<view class="briefing-box">
							<view class="brief-item">
								<text class="brief-label">首服提醒</text>
								<text class="brief-val primary">08:00 (XX药)</text>
							</view>
							<view class="brief-item">
								<text class="brief-label">睡眠简报</text>
								<text class="brief-val danger">深度睡眠不足 (42%)</text>
							</view>
						</view>
						<text class="advice">建议：今日避免剧烈运动，白天适当午休。</text>
					</view>
				</view>

				<view class="user-msg-group">
					<view class="chat-bubble-user card-shadow">
						<image class="msg-img" src="https://lh3.googleusercontent.com/aida-public/AB6AXuDL6E5eLGjB4xMebqv3mrM9OHQ0L2Or6kd18f2GHbx4uyZyqKEh2onhMwdTtiYMrnx17wXmQoRoFsvpGPInLwvYbL7wZtSrN9xHCBk5_LTb1pNE5Z17_Yz7YzDrJGVJ60-QIJV5itlqCJbFioOZyDz_hMY9ZTP-yj1DDy5DsJY_nWhAXHcdyuW3j1rGxfB76i0OAdgd2NBv9LIh1jGjNP-Gb9L52glprn9LDaMZttjNQzWs7ptB64ydqgol7xmTx3V2OdmpgLtGTFE" mode="aspectFill"></image>
						<text class="user-text">帮我添加这个药。</text>
					</view>
				</view>

				<view class="ai-msg-group">
					<view class="chat-bubble-ai card-shadow">
						<view class="result-header">
							<text class="result-title">已识别: XX药</text>
							<image class="icon" src="../../static/Home/check.svg"/>
							<view class="result-tag">每日两次 · 随餐服用</view>
						</view>
						<text class="msg-text">我根据您的餐食时间（如早9点、晚6点）来设置具体提醒吗？</text>
						<view class="action-btns">
							<button class="primary-btn">按默认餐时设置</button>
							<button class="outline-btn">手动修改时间</button>
						</view>
					</view>
				</view>

				<view class="ai-msg-group" id="msg-last">
					<view class="chat-bubble-ai card-shadow">
						<view class="chart-header">
							<view>
								<text class="chart-title">近7日血压趋势</text>
								<text class="chart-unit">单位: mmHg</text>
							</view>
							<view class="live-tag">实时数据</view>
						</view>
						
						<view class="chart-container-inner">
							<svg class="chart-svg" viewBox="0 0 200 60">
								<path d="M0 45 Q 40 40, 60 50 T 100 45 T 140 35 T 200 40" fill="none" stroke="#3B82F6" stroke-width="2"></path>
								<circle cx="140" cy="35" fill="#3B82F6" r="3"></circle>
							</svg>
							<view class="chart-popover">
								<text class="pop-val">118/76</text>
								<text class="pop-time">昨日 18:00</text>
							</view>
						</view>

						<view class="chart-actions">
							<view class="chart-btn-gray">详细报告</view>
							<view class="chart-btn-primary">通知家属</view>
						</view>
					</view>
				</view>
				
			</view>
		</scroll-view>

		<view class="footer">
			<scroll-view scroll-x class="shortcut-scroll" show-scrollbar="false">
				<view class="shortcut-item">
					<image class="icon" src="../../static/Health/pill-active.svg"/>
					<text>询问副作用</text>
				</view>
				<view class="shortcut-item">
					<image class="icon" src="../../static/Home/warning.svg"/>
					<text>报告不适</text>
				</view>
				<view class="shortcut-item">
					<image class="icon" src="../../static/Mine/report.svg"/>
					<text>就医清单</text>
				</view>
			</scroll-view>

			<view class="input-row">
				<image class="icon" src="../../static/Health/plus-circle.svg"/>
				<view class="input-box">
					<input class="real-input" placeholder="输入健康疑问..." placeholder-class="ph" />
					<image class="icon" src="../../static/ai/microphone.svg"/>
				</view>
				<view class="send-btn">
					<image class="icon" src="../../static/ai/send.svg"/>
				</view>
			</view>
			<view class="home-bar"></view>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			lastMsgId: 'msg-last'
		}
	}
}
</script>

<style lang="scss" scoped>
// 基础变量与暗黑适配
$primary: #3B82F6;
$bg-light: #F8FAFC;
$bg-dark: #0F172A;

.chat-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: #F1F5F9;
    overflow: hidden;
	@media (prefers-color-scheme: dark) { background-color: #020617; }
}

.status-bar { height: var(--status-bar-height); background: #fff; @media (prefers-color-scheme: dark) { background: $bg-dark; } }

/* Header */
.header {
	padding: 20rpx 40rpx;
	background: rgba(255, 255, 255, 0.85);
	backdrop-filter: blur(20px);
	display: flex;
	justify-content: space-between;
	align-items: center;
	border-bottom: 1rpx solid #e2e8f0;
	z-index: 100;
	@media (prefers-color-scheme: dark) { background: rgba(15, 23, 42, 0.85); border-color: #1e293b; color: #fff; }

	.header-left {
		display: flex;
		align-items: center;
		gap: 20rpx;
		.ai-avatar {
			width: 80rpx; height: 80rpx; background: $primary; color: #fff;
			border-radius: 50%; display: flex; align-items: center; justify-content: center;
			font-weight: bold; font-size: 32rpx; box-shadow: 0 8rpx 20rpx rgba(59, 130, 246, 0.2);
		}
		.header-title { font-size: 34rpx; font-weight: 700; }
		.online-status {
			display: flex; align-items: center; gap: 8rpx;
			.dot { width: 12rpx; height: 12rpx; background: #22c55e; border-radius: 50%; }
			.status-text { font-size: 22rpx; color: #16a34a; font-weight: 500; }
		}
	}
}

/* 聊天主体 */
.chat-main {
    flex: 1;          /* 核心：占据 Header 和 Footer 之外的所有剩余空间 */
    height: 0;        /* 关键：在一些平台下，必须设为 0 才能正确触发内部滚动 */
    overflow-y: auto; /* 允许纵向滑动 */
    
    .chat-content { 
        padding: 40rpx 30rpx; 
        /* 如果你想让最后一条消息不贴着输入框，可以加一个底部内边距 */
        padding-bottom: 60rpx; 
    }
}

.ai-msg-group, .user-msg-group { margin-bottom: 40rpx; display: flex; flex-direction: column; }
.user-msg-group { align-items: flex-end; }

.chat-bubble-ai, .chat-bubble-user {
	padding: 30rpx;
	max-width: 85%;
	border-radius: 32rpx;
	font-size: 28rpx;
	line-height: 1.6;
}

.chat-bubble-ai {
	background: #fff;
	border-bottom-left-radius: 8rpx;
	color: #1e293b;
	@media (prefers-color-scheme: dark) { background: #1e293b; color: #f1f5f9; }
}

.chat-bubble-user {
	background: $primary;
	border-bottom-right-radius: 8rpx;
	color: #fff;
	box-shadow: 0 10rpx 20rpx rgba(59, 130, 246, 0.3);
}

/* 特殊卡片内容 */
.msg-text-bold { font-weight: 600; margin-bottom: 20rpx; display: block; .highlight { color: $primary; font-weight: 800; } }

.briefing-box {
	background: #f8fafc;
	padding: 20rpx;
	border-radius: 20rpx;
	margin: 20rpx 0;
	@media (prefers-color-scheme: dark) { background: #0f172a; }
	.brief-item {
		display: flex; justify-content: space-between; margin-bottom: 10rpx;
		.brief-label { font-size: 22rpx; color: #64748b; }
		.brief-val { font-size: 22rpx; font-weight: 700; }
		.primary { color: $primary; }
		.danger { color: #ef4444; }
	}
}

.advice { font-size: 24rpx; font-style: italic; color: #64748b; @media (prefers-color-scheme: dark) { color: #94a3b8; } }

.msg-img { width: 100%; height: 240rpx; border-radius: 20rpx; margin-bottom: 16rpx; }

/* 识别结果卡片 */
.result-header {
	display: flex; flex-wrap: wrap; align-items: center; gap: 12rpx; margin-bottom: 20rpx;
	.result-title { font-weight: 700; }
	.success { color: #22c55e; font-size: 32rpx; }
	.result-tag { background: rgba(59, 130, 246, 0.1); color: $primary; font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 100rpx; font-weight: 700; }
}

.action-btns {
	margin-top: 30rpx;
	display: flex; flex-direction: column; gap: 16rpx;
	button { width: 100%; height: 80rpx; line-height: 80rpx; font-size: 26rpx; border-radius: 20rpx; font-weight: 600; }
	.primary-btn { background: $primary; color: #fff; }
	.outline-btn { background: transparent; border: 1rpx solid #e2e8f0; color: #64748b; @media (prefers-color-scheme: dark) { border-color: #334155; color: #cbd5e1; } }
}

/* 图表样式 */
.chart-header {
	display: flex; justify-content: space-between; margin-bottom: 30rpx;
	.chart-title { font-size: 28rpx; font-weight: 700; display: block; }
	.chart-unit { font-size: 20rpx; color: #94a3b8; }
	.live-tag { font-size: 18rpx; background: rgba(34, 197, 94, 0.1); color: #22c55e; padding: 4rpx 12rpx; border-radius: 8rpx; font-weight: 700;height: 36rpx;display: flex;align-items: center;justify-content: center; }
}
.chart-container-inner {
	height: 120rpx; position: relative;
	.chart-svg { width: 100%; height: 100%; }
	.chart-popover {
		position: absolute; top: -10rpx; right: 20rpx; background: $primary; color: #fff;
		padding: 8rpx 16rpx; border-radius: 16rpx; display: flex; flex-direction: column;
		.pop-val { font-size: 22rpx; font-weight: 700; }
		.pop-time { font-size: 16rpx; opacity: 0.8; }
	}
}
.chart-actions {
	display: flex; gap: 16rpx; margin-top: 20rpx;
	view { flex: 1; height: 60rpx; line-height: 60rpx; text-align: center; border-radius: 12rpx; font-size: 22rpx; font-weight: 700; }
	.chart-btn-gray { background: #f1f5f9; color: #64748b; @media (prefers-color-scheme: dark) { background: #334155; color: #f1f5f9; } }
	.chart-btn-primary { background: $primary; color: #fff; }
}

/* Footer 输入框 */
.footer {
	padding: 20rpx 30rpx;
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: blur(20px);
	border-top: 1rpx solid #e2e8f0;
	@media (prefers-color-scheme: dark) { background: rgba(15, 23, 42, 0.9); border-color: #1e293b; }

	.shortcut-scroll {
		white-space: nowrap; margin-bottom: 24rpx;
		.shortcut-item {
			display: inline-flex; align-items: center; gap: 8rpx; padding: 12rpx 24rpx;
			background: #fff; border: 1rpx solid #e2e8f0; border-radius: 100rpx; margin-right: 16rpx;
			font-size: 24rpx; font-weight: 600; color: #475569;
			@media (prefers-color-scheme: dark) { background: #1e293b; border-color: #334155; color: #f1f5f9; }
			.emoji { font-size: 28rpx; }
			.material-icons { font-size: 28rpx; &.warn { color: #f59e0b; } &.info { color: $primary; } }
		}
	}

	.input-row {
		display: flex; align-items: center; gap: 24rpx; padding-bottom: 20rpx;
		.add-icon { color: #94a3b8; font-size: 48rpx; }
		.input-box {
			flex: 1; background: #f1f5f9; border-radius: 100rpx; padding: 0 30rpx;
			display: flex; align-items: center; height: 84rpx;
			@media (prefers-color-scheme: dark) { background: #334155; }
			.real-input { flex: 1; font-size: 28rpx; color: #1e293b; @media (prefers-color-scheme: dark) { color: #fff; } }
			.mic-icon { color: #94a3b8; font-size: 40rpx; }
		}
		.send-btn {
			width: 84rpx; height: 84rpx; background: $primary; border-radius: 50%;
			display: flex; align-items: center; justify-content: center; color: #fff;
			box-shadow: 0 8rpx 20rpx rgba(59, 130, 246, 0.3);
		}
	}
}

.home-bar { width: 240rpx; height: 8rpx; background: #e2e8f0; border-radius: 100rpx; margin: 10rpx auto 0; @media (prefers-color-scheme: dark) { background: #334155; } }

.card-shadow { box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.03); }
.material-icons { font-family: 'Material Icons'; }
</style>