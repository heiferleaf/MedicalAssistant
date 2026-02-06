<template>
	<view class="container">
		<view class="status-bar"></view>

		<header class="header">
			<view class="header-content">
				<view class="title-group">
					<text class="title">我的药箱</text>
					<text class="subtitle">共管理 {{ medicines.length }} 种药品</text>
				</view>
				<view class="add-btn" @tap="addMedicine">
					<image class="icon" src="../../static/Health/plus-circle.svg"/>
				</view>
			</view>
		</header>

    <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
      <view class="filter-container">
        <view 
          v-for="(cat, index) in categories" 
          :key="index"
          class="filter-item"
          :class="{ 'active': activeCategory === index }"
          @tap="activeCategory = index"
        >
          <text>{{ cat }}</text>
        </view>
      </view>
    </scroll-view>

		<scroll-view scroll-y class="medicine-list">
			<view class="list-wrapper">
				<view 
					v-for="(item, index) in filteredMedicines" 
					:key="index" 
					class="medicine-card"
					@tap="toDetail(item)"
				>
					<view class="icon-box" :style="{ backgroundColor: item.bgColor }">
						<image class="icon" :style="{ color: item.iconColor }" :src="item.icon" style="height: 56rpx;width: 56rpx;"/>
					</view>

					<view class="info-box">
						<text class="med-name">{{ item.name }}</text>
						<text class="med-desc">剩余 {{ item.stock }}{{ item.unit }} · {{ item.frequency }}</text>
					</view>

					<view class="status-tag">
						<text :class="['tag-text', getStockStatusClass(item.stockStatus)]">
							{{ item.stockStatus }}
						</text>
					</view>
				</view>
			</view>
		</scroll-view>

		<app-navbar :current="1" @change="handleTabChange" />
	</view>
</template>

<script>
export default {
	data() {
		return {
			activeCategory: 0,
			categories: ["全部药品", "日常服用", "感冒发烧", "常备药"],
			medicines: [
				{
					name: "阿司匹林肠溶片",
					stock: 12,
					unit: "片",
					frequency: "每天 1 次",
					stockStatus: "库存充足",
					icon: "../../static/Health/pill-active.svg",
					iconColor: "#3b82f6",
					bgColor: "rgba(59, 130, 246, 0.1)"
				},
				{
					name: "维生素 C 咀嚼片",
					stock: 5,
					unit: "粒",
					frequency: "每天 2 次",
					stockStatus: "库存紧张",
					icon: "../../static/Health/medication.svg",
					iconColor: "#f59e0b",
					bgColor: "rgba(245, 158, 11, 0.1)"
				},
				{
					name: "布洛芬缓释胶囊",
					stock: 20,
					unit: "粒",
					frequency: "必要时服用",
					stockStatus: "备用中",
					icon: "../../static/Health/science.svg",
					iconColor: "#a855f7",
					bgColor: "rgba(168, 85, 247, 0.1)"
				},
				{
					name: "降压灵",
					stock: 2,
					unit: "片",
					frequency: "每天 1 次",
					stockStatus: "即将用完",
					icon: "../../static/Health/vaccines.svg",
					iconColor: "#f43f5e",
					bgColor: "rgba(244, 63, 94, 0.1)"
				}
			]
		};
	},
	computed: {
		filteredMedicines() {
			// 这里可以根据 activeCategory 实现过滤逻辑
			return this.medicines;
		}
	},
	methods: {
		getStockStatusClass(status) {
			const map = {
				"库存充足": "status-ok",
				"库存紧张": "status-warning",
				"即将用完": "status-danger",
				"备用中": "status-normal"
			};
			return map[status] || "";
		},
		handleTabChange(index) {
			console.log("切换到页签：", index);
		},
		addMedicine() {
			uni.navigateTo({ url: '/pages/medicine/add' });
		},
		toDetail(item) {
			console.log("查看详情", item.name);
		}
	}
};
</script>

<style lang="scss" scoped>
.container {
	min-height: 100vh;
	background-color: #f8fafc;
	/* 暗黑模式适配 */
	@media (prefers-color-scheme: dark) { background-color: #0f172a; }
}

.status-bar { height: var(--status-bar-height); }

.header {
	padding: 40rpx 40rpx 30rpx;
	.header-content {
		display: flex;
		justify-content: space-between;
		align-items: center;
		.title {
			font-size: 48rpx;
			font-weight: bold;
			color: #1e293b;
			@media (prefers-color-scheme: dark) { color: #fff; }
		}
		.subtitle {
			font-size: 26rpx;
			color: #64748b;
			margin-top: 8rpx;
			display: block;
		}
		.add-btn {
			width: 80rpx;
			height: 80rpx;
			background: #fff;
			border-radius: 24rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
			@media (prefers-color-scheme: dark) { background: #1e293b; }
			.icon-add { font-size: 40rpx; color: #64748b; }
		}
	}
}

.filter-scroll {
	white-space: nowrap;
	padding: 0 40rpx;
	margin-bottom: 40rpx;
	width: 100%;
	
	.filter-container {
		display: flex;
		gap: 24rpx;
		padding-right: 40rpx; // 留出边缘距离
    padding: 10rpx 40rpx; // 在这里设置左右边距，滑动时文字可以贴边
	}
	
	.filter-item {
		padding: 16rpx 40rpx;
		background: #fff;
		border: 2rpx solid #f1f5f9;
		border-radius: 40rpx;
		font-size: 26rpx;
		color: #64748b;
		transition: all 0.3s;
		flex-shrink: 0;      // 绝对不缩放
    display: flex;       // 内部居中
    align-items: center;
    justify-content: center;
    white-space: nowrap; // 绝对不换行
    
    padding: 16rpx 40rpx;
    background: #fff;
		@media (prefers-color-scheme: dark) { 
			background: #1e293b; 
			border-color: #334155; 
			color: #94a3b8;
		}
		
		&.active {
			background: #6366f1;
			border-color: #6366f1;
			color: #fff;
			font-weight: 600;
		}
	}
}

.medicine-list {
	height: calc(100vh - 450rpx); // 减去头部和导航栏高度
	.list-wrapper { padding: 0 40rpx 200rpx; }
}

.medicine-card {
	background: #fff;
	padding: 30rpx;
	border-radius: 40rpx;
	display: flex;
	align-items: center;
	gap: 30rpx;
	margin-bottom: 24rpx;
	border: 2rpx solid #f1f5f9;
	box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.02);
	
	@media (prefers-color-scheme: dark) { 
		background: #1e293b; 
		border-color: #334155; 
	}

	.icon-box {
		width: 120rpx;
		height: 120rpx;
		border-radius: 32rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		.iconfont { font-size: 60rpx; }
	}

	.info-box {
		flex: 1;
		.med-name {
			font-size: 34rpx;
			font-weight: bold;
			color: #1e293b;
			display: block;
			@media (prefers-color-scheme: dark) { color: #fff; }
		}
		.med-desc {
			font-size: 24rpx;
			color: #94a3b8;
			margin-top: 8rpx;
			display: block;
		}
	}

	.status-tag {
		.tag-text {
			font-size: 20rpx;
			font-weight: bold;
			padding: 6rpx 16rpx;
			border-radius: 8rpx;
		}
		
		.status-ok { background: rgba(16, 185, 129, 0.1); color: #10b981; }
		.status-warning { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
		.status-danger { background: rgba(244, 63, 94, 0.1); color: #f43f5e; }
		.status-normal { background: #f1f5f9; color: #64748b; @media (prefers-color-scheme: dark) { background: #334155; color: #94a3b8; } }
	}
	
	&:active { transform: scale(0.98); }
}

/* 图标库占位 */
.iconfont {
	font-family: "Material Symbols Outlined"; /* 请确保在 App.vue 引入了对应字体 */
}
</style>