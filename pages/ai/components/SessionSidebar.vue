<template>
	<view v-if="visible" class="sidebar" :class="{ 'show': visible }">
		<view class="sidebar-header">
			<text class="sidebar-title">会话列表</text>
			<view class="sidebar-actions">
				<view class="action-btn new-btn" @click="$emit('new')">
					<text class="btn-text">+</text>
				</view>
				<view class="action-btn close-btn" @click="$emit('close')">
					<text class="btn-text">×</text>
				</view>
			</view>
		</view>
		<scroll-view scroll-y class="session-list">
			<view 
				v-for="(session, index) in sessions" 
				:key="index" 
				class="session-item"
				:class="{ 'active': session.sessionId === currentSessionId }"
				@click="$emit('switch', session)"
			>
				<view class="session-info">
					<text class="session-title">{{ session.summary || '新会话' }}</text>
					<text class="session-preview">{{ session.lastMessage || '暂无消息' }}</text>
					<text v-if="session.unread" class="unread-tag">{{ session.unread }}</text>
				</view>
				<view class="session-delete" @click.stop="$emit('delete', session, index)">
					<text>🗑️</text>
				</view>
			</view>
			<view v-if="sessions.length === 0" class="empty-tip">
				<text>暂无会话</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
export default {
	name: 'SessionSidebar',
	props: {
		visible: {
			type: Boolean,
			default: false
		},
		sessions: {
			type: Array,
			default: () => []
		},
		currentSessionId: {
			type: String,
			default: ''
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #3B82F6;

.sidebar {
	width: 300px;
	height: 100vh;
	background: #fff;
	border-right: 1rpx solid #e2e8f0;
	display: flex;
	flex-direction: column;
	position: absolute;
	left: 0;
	top: 0;
	z-index: 999;
	transition: transform 0.3s ease;
	transform: translateX(-100%);
	
	&.show {
		transform: translateX(0);
	}
	
	@media (prefers-color-scheme: dark) { 
		background: #1e293b; 
		border-color: #334155;
	}
	
	.sidebar-header {
		padding: 30rpx;
		border-bottom: 1rpx solid #e2e8f0;
		display: flex;
		justify-content: space-between;
		align-items: center;
		@media (prefers-color-scheme: dark) { border-color: #334155; }
		
		.sidebar-title {
			font-size: 32rpx;
			font-weight: 700;
			color: #1e293b;
			@media (prefers-color-scheme: dark) { color: #f1f5f9; }
		}
		
		.sidebar-actions {
			display: flex;
			gap: 16rpx;
			
			.action-btn {
				width: 60rpx;
				height: 60rpx;
				border-radius: 50%;
				display: flex;
				align-items: center;
				justify-content: center;
				cursor: pointer;
				transition: all 0.2s;
				
				&.new-btn {
					background: $primary;
					
					.btn-text {
						color: #fff;
						font-size: 32rpx;
						font-weight: bold;
					}
				}
				
				&.close-btn {
					background: rgba(0, 0, 0, 0.1);
					
					.btn-text {
						color: #1e293b;
						font-size: 32rpx;
						font-weight: bold;
					}
					
					@media (prefers-color-scheme: dark) {
						background: rgba(255, 255, 255, 0.1);
						
						.btn-text {
							color: #f1f5f9;
						}
					}
				}
			}
		}
	}
	
	.session-list {
		flex: 1;
		overflow-y: auto;
		
		.session-item {
			padding: 30rpx;
			border-bottom: 1rpx solid #e2e8f0;
			display: flex;
			justify-content: space-between;
			align-items: center;
			cursor: pointer;
			transition: background 0.2s;
			@media (prefers-color-scheme: dark) { border-color: #334155; }
			
			&:active {
				background: #f1f5f9;
				@media (prefers-color-scheme: dark) { background: #334155; }
			}
			
			&.active {
				background: rgba(59, 130, 246, 0.1);
				border-left: 4rpx solid $primary;
			}
			
			.session-info {
				flex: 1;
				overflow: hidden;
				display: flex;
				align-items: center;
				gap: 12rpx;
				
				.session-title {
					font-size: 28rpx;
					font-weight: 600;
					color: #1e293b;
					display: block;
					@media (prefers-color-scheme: dark) { color: #f1f5f9; }
				}
				
				.session-preview {
					font-size: 24rpx;
					color: #64748b;
					display: block;
					overflow: hidden;
					text-overflow: ellipsis;
					white-space: nowrap;
					flex: 1;
					@media (prefers-color-scheme: dark) { color: #94a3b8; }
				}
				
				.unread-tag {
					background: $primary;
					color: #fff;
					font-size: 20rpx;
					padding: 4rpx 12rpx;
					border-radius: 100rpx;
					font-weight: 600;
				}
			}
			
			.session-delete {
				padding: 12rpx 20rpx;
				font-size: 28rpx;
				color: #ef4444;
				background: rgba(239, 68, 68, 0.1);
				border-radius: 8rpx;
				opacity: 0;
				transition: all 0.2s;
				display: flex;
				align-items: center;
				justify-content: center;
				font-weight: 600;
				
				.session-item:active & {
					opacity: 1;
					background: rgba(239, 68, 68, 0.2);
				}
			}
		}
		
		.empty-tip {
			padding: 60rpx 30rpx;
			text-align: center;
			color: #94a3b8;
			font-size: 28rpx;
		}
	}
}
</style>
