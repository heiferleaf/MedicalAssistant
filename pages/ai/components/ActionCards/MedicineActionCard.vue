<template>
	<view :class="['medicine-action-card', statusClass]">
		<!-- 卡片头部 -->
		<view class="card-header">
			<text class="card-title">{{ statusText }}</text>
		</view>

		<!-- 卡片内容 -->
		<view class="card-content">
			<!-- 待确认状态：可编辑 -->
			<template v-if="!status || status === 'pending'">
				<view class="info-row">
					<text class="label">药品名称</text>
					<input 
						class="edit-input" 
						v-model="editableData.medicineName"
						placeholder="请输入药品名称"
					/>
				</view>
				<view class="info-row">
					<text class="label">默认剂量</text>
					<input 
						class="edit-input" 
						v-model="editableData.defaultDosage"
						placeholder="例如：1 片/次"
					/>
				</view>
				<view class="info-row">
					<text class="label">备注</text>
					<input 
						class="edit-input" 
						v-model="editableData.remark"
						placeholder="可选，如：饭后服用"
					/>
				</view>
			</template>
			
			<!-- 已确认/已取消状态：只读显示 -->
			<template v-else>
				<view class="info-row">
					<text class="label">药品名称</text>
					<text class="value">{{ editableData.medicineName || '-' }}</text>
				</view>
				<view class="info-row">
					<text class="label">默认剂量</text>
					<text class="value">{{ editableData.defaultDosage || '-' }}</text>
				</view>
				<view class="info-row">
					<text class="label">备注</text>
					<text class="value">{{ editableData.remark || '-' }}</text>
				</view>
			</template>
		</view>

		<!-- 待确认状态：取消 + 确认按钮 -->
		<view class="card-actions" v-if="!status || status === 'pending'">
			<button 
				class="btn btn-cancel" 
				hover-class="btn-cancel-hover"
				@click="handleCancel"
			>
				取消
			</button>
			
			<button 
				class="btn btn-confirm" 
				hover-class="btn-confirm-hover"
				@click="handleConfirm"
			>
				确认
			</button>
		</view>
		
		<!-- 已确认/已取消状态：显示状态标签 -->
		<view class="card-status" v-else>
			<text :class="['status-badge', status === 'confirmed' ? 'status-confirmed' : 'status-cancelled']">
				{{ status === 'confirmed' ? '✓ 已添加' : '✕ 已取消' }}
			</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'MedicineActionCard',
	props: {
		actionData: {
			type: Object,
			default: () => ({})
		},
		status: {
			type: String,
			default: 'pending'
		},
		messageId: {
			type: String,
			default: ''
		}
	},
	data() {
		return {
			editableData: {}
		};
	},
	computed: {
		statusClass() {
			if (this.status === 'confirmed') return 'card-confirmed';
			if (this.status === 'cancelled') return 'card-cancelled';
			return '';
		},
		statusText() {
			if (this.status === 'confirmed') return '药箱 - 已添加';
			if (this.status === 'cancelled') return '药箱 - 已取消';
			return '药箱';
		}
	},
	watch: {
		actionData: {
			immediate: true,
			handler(newData) {
				this.editableData = { ...newData };
			}
		}
	},
	methods: {
		handleCancel() {
			this.$emit('cancel', { 
				...this.editableData,
				messageId: this.messageId
			});
		},
		
		handleConfirm() {
			this.$emit('confirm', { 
				...this.editableData,
				messageId: this.messageId
			});
		}
	}
}
</script>

<style lang="scss" scoped>
$primary: #6366F1;
$primary-light: rgba(99, 102, 241, 0.1);
$success: #10B981;
$success-light: rgba(16, 185, 129, 0.1);
$warning: #F59E0B;
$warning-light: rgba(245, 158, 11, 0.1);
$danger: #EF4444;
$danger-light: rgba(239, 68, 68, 0.1);
$bg-light: #F8FAFC;
$text-primary: #1E293B;
$text-secondary: #64748B;

.medicine-action-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 0;
	margin: 24rpx 0;
	box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
	border: 2rpx solid #F1F5F9;
	overflow: hidden;
	
	@media (prefers-color-scheme: dark) {
		background: #1E293B;
		border-color: #334155;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.3);
	}
}

.card-header {
	padding: 24rpx 32rpx;
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	
	.card-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #fff;
		letter-spacing: 2rpx;
	}
}

.card-content {
	padding: 32rpx;
	
	.info-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 16rpx 0;
		border-bottom: 1rpx solid #E2E8F0;
		
		@media (prefers-color-scheme: dark) {
			border-bottom-color: #334155;
		}
		
		&:last-child {
			border-bottom: none;
		}
		
		.label {
			font-size: 26rpx;
			color: $text-secondary;
			flex-shrink: 0;
			width: 140rpx;
		}
		
		.value {
			font-size: 28rpx;
			font-weight: 600;
			color: $text-primary;
			
			@media (prefers-color-scheme: dark) {
				color: #F1F5F9;
			}
		}
		
		.edit-input {
			flex: 1;
			font-size: 28rpx;
			color: $text-primary;
			text-align: right;
			padding: 8rpx 16rpx;
			background: #F8FAFC;
			border-radius: 8rpx;
			
			@media (prefers-color-scheme: dark) {
				color: #F1F5F9;
				background: #334155;
			}
		}
	}
}

.card-actions {
	display: flex;
	gap: 16rpx;
	padding: 24rpx 32rpx;
	background: $bg-light;
	
	@media (prefers-color-scheme: dark) {
		background: #0F172A;
	}
}

.btn {
	flex: 1;
	height: 80rpx;
	border-radius: 12rpx;
	border: none;
	font-size: 28rpx;
	font-weight: 600;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0;
	
	&.btn-confirm {
		background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
		color: #fff;
		box-shadow: 0 8rpx 16rpx rgba(16, 185, 129, 0.3);
	}
	
	&.btn-cancel {
		background: #fff;
		color: $text-secondary;
		border: 2rpx solid #E2E8F0;
		
		@media (prefers-color-scheme: dark) {
			background: #334155;
			border-color: #475569;
		}
	}
}

.btn-confirm-hover {
	opacity: 0.9;
	transform: scale(0.98);
}

.btn-cancel-hover {
	background: #F1F5F9;
	
	@media (prefers-color-scheme: dark) {
		background: #475569;
	}
}

.card-status {
	padding: 24rpx 32rpx;
	background: $bg-light;
	text-align: center;
	
	@media (prefers-color-scheme: dark) {
		background: #0F172A;
	}
}

.status-badge {
	display: inline-block;
	padding: 8rpx 24rpx;
	border-radius: 20rpx;
	font-size: 24rpx;
	font-weight: 600;
	
	&.status-confirmed {
		background: $success-light;
		color: $success;
	}
	
	&.status-cancelled {
		background: rgba(239, 68, 68, 0.1);
		color: #EF4444;
	}
}

.card-confirmed {
	.card-header {
		background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	}
}

.card-cancelled {
	.card-header {
		background: linear-gradient(135deg, #6B7280 0%, #9CA3AF 100%);
	}
}
</style>
