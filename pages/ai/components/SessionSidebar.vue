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
					<view class="session-title-wrapper">
						<text class="session-title">{{ session.summary || '新会话' }}</text>
						<view class="edit-btn" @click.stop="openEditModal(session, index)">
							<text class="edit-text">编辑</text>
						</view>
					</view>
					<text class="session-preview">{{ session.lastMessage || '暂无消息' }}</text>
					<text v-if="session.unread" class="unread-tag">{{ session.unread }}</text>
				</view>
				<view class="session-delete" @click.stop="$emit('delete', session, index)">
					<text class="delete-icon">🗑️</text>
				</view>
			</view>
			<view v-if="sessions.length === 0" class="empty-tip">
				<text>暂无会话</text>
			</view>
		</scroll-view>
		
		<!-- 自定义编辑弹窗 -->
		<view v-if="showEditModal" class="modal-mask" @click="closeEditModal">
			<view class="modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">修改会话名称</text>
					<view class="modal-close" @click="closeEditModal">
						<text class="close-icon">×</text>
					</view>
				</view>
				<view class="modal-body">
					<input 
						class="modal-input" 
						v-model="editName" 
						placeholder="请输入会话名称"
						maxlength="20"
						focus
					/>
				</view>
				<view class="modal-footer">
					<view class="modal-btn cancel-btn" @click="closeEditModal">
						<text class="btn-text">取消</text>
					</view>
					<view class="modal-btn confirm-btn" @click="confirmEdit">
						<text class="btn-text">确定</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'SessionSidebar',
	data() {
		return {
			showEditModal: false,
			editName: '',
			editSession: null,
			editIndex: -1
		};
	},
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
	},
	watch: {
		visible(newVal) {
			if (!newVal) {
				this.closeEditModal();
			}
		}
	},
	methods: {
		// 打开编辑弹窗
		openEditModal(session, index) {
			this.editSession = session;
			this.editIndex = index;
			this.editName = session.summary || '新会话';
			this.showEditModal = true;
		},
		
		// 关闭编辑弹窗
		closeEditModal() {
			this.showEditModal = false;
			this.editSession = null;
			this.editIndex = -1;
			this.editName = '';
		},
		
		// 确认编辑
		confirmEdit() {
			if (this.editName && this.editName.trim()) {
				this.$emit('update-name', { 
					session: this.editSession, 
					index: this.editIndex, 
					newName: this.editName.trim() 
				});
				this.closeEditModal();
			}
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #6366F1;
$primary-light: rgba(99, 102, 241, 0.1);
$text-primary: #1E293B;
$text-secondary: #64748B;
$border-color: #E2E8F0;
$bg-light: #F8FAFC;

.sidebar {
	width: 25vw;
	min-width: 200px;
	max-width: 320px;
	height: 100vh;
	background: #fff;
	border-right: 1rpx solid $border-color;
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
		background: #1E293B; 
		border-color: #334155;
	}
	
	.sidebar-header {
		padding: 30rpx;
		border-bottom: 1rpx solid $border-color;
		display: flex;
		justify-content: space-between;
		align-items: center;
		@media (prefers-color-scheme: dark) { border-color: #334155; }
		
		.sidebar-title {
			font-size: 32rpx;
			font-weight: 700;
			color: $text-primary;
			@media (prefers-color-scheme: dark) { color: #F1F5F9; }
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
						color: $text-primary;
						font-size: 32rpx;
						font-weight: bold;
					}
					
					@media (prefers-color-scheme: dark) {
						background: rgba(255, 255, 255, 0.1);
						
						.btn-text {
							color: #F1F5F9;
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
			border-bottom: 1rpx solid $border-color;
			display: flex;
			justify-content: space-between;
			align-items: center;
			cursor: pointer;
			transition: background 0.2s;
			@media (prefers-color-scheme: dark) { border-color: #334155; }
			
			&:active {
				background: #F1F5F9;
				@media (prefers-color-scheme: dark) { background: #334155; }
			}
			
			&.active {
				background: $primary-light;
				border-left: 4rpx solid $primary;
			}
			
			.session-info {
				flex: 1;
				overflow: hidden;
				display: flex;
				flex-direction: column;
				gap: 8rpx;
				
				.session-title-wrapper {
					display: flex;
					align-items: center;
					gap: 12rpx;
					
					.session-title {
						font-size: 28rpx;
						font-weight: 600;
						color: $text-primary;
						flex: 1;
						overflow: hidden;
						text-overflow: ellipsis;
						white-space: nowrap;
						@media (prefers-color-scheme: dark) { color: #F1F5F9; }
					}
					
					.edit-btn {
						padding: 4rpx 12rpx;
						background: $primary-light;
						border-radius: 8rpx;
						flex-shrink: 0;
						
						.edit-text {
							font-size: 22rpx;
							color: $primary;
						}
					}
				}
				
				.session-preview {
					font-size: 24rpx;
					color: $text-secondary;
					overflow: hidden;
					text-overflow: ellipsis;
					white-space: nowrap;
				}
				
				.unread-tag {
					display: inline-block;
					padding: 4rpx 12rpx;
					background: $primary;
					color: #fff;
					font-size: 20rpx;
					border-radius: 12rpx;
					margin-top: 4rpx;
					align-self: flex-start;
				}
			}
			
			.session-delete {
				padding: 16rpx;
				opacity: 0.6;
				transition: opacity 0.2s;
				
				&:active {
					opacity: 1;
				}
				
				.delete-icon {
					font-size: 28rpx;
				}
			}
		}
		
		.empty-tip {
			padding: 60rpx;
			text-align: center;
			color: $text-secondary;
			font-size: 28rpx;
		}
	}
}

.modal-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 1000;
}

.modal-content {
	width: 600rpx;
	background: #fff;
	border-radius: 24rpx;
	overflow: hidden;
	box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.2);
	
	@media (prefers-color-scheme: dark) {
		background: #1E293B;
	}
	
	.modal-header {
		padding: 32rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;
		border-bottom: 1rpx solid $border-color;
		
		@media (prefers-color-scheme: dark) {
			border-color: #334155;
		}
		
		.modal-title {
			font-size: 32rpx;
			font-weight: 600;
			color: $text-primary;
			
			@media (prefers-color-scheme: dark) {
				color: #F1F5F9;
			}
		}
		
		.modal-close {
			width: 56rpx;
			height: 56rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			
			.close-icon {
				font-size: 40rpx;
				color: $text-secondary;
			}
		}
	}
	
	.modal-body {
		padding: 32rpx;
		
		.modal-input {
			width: 100%;
			height: 80rpx;
			padding: 0 24rpx;
			background: $bg-light;
			border: 2rpx solid $border-color;
			border-radius: 16rpx;
			font-size: 28rpx;
			color: $text-primary;
			box-sizing: border-box;
			
			@media (prefers-color-scheme: dark) {
				background: #334155;
				border-color: #475569;
				color: #F1F5F9;
			}
			
			&:focus {
				border-color: $primary;
			}
		}
	}
	
	.modal-footer {
		padding: 24rpx 32rpx;
		display: flex;
		gap: 24rpx;
		border-top: 1rpx solid $border-color;
		
		@media (prefers-color-scheme: dark) {
			border-color: #334155;
		}
		
		.modal-btn {
			flex: 1;
			height: 80rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			border-radius: 16rpx;
			font-size: 28rpx;
			font-weight: 600;
			
			&.cancel-btn {
				background: $bg-light;
				color: $text-secondary;
				border: 2rpx solid $border-color;
				
				@media (prefers-color-scheme: dark) {
					background: #334155;
					border-color: #475569;
					color: #F1F5F9;
				}
			}
			
			&.confirm-btn {
				background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%);
				color: #fff;
			}
		}
	}
}
</style>
