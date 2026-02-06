<template>
	<view class="tab-bar">
		<view class="nav-container">
			<view 
				v-for="(item, index) in tabs" 
				:key="index"
				class="nav-item" 
				:class="{ 'active': current === index }"
				@tap="switchTab(index)"
			>
				<view class="nav-icon-wrapper">
					<image 
						:src="item.icon" 
						mode="aspectFit" 
						class="nav-icon"
						:class="{ 'icon-active': current === index }"
					></image>
				</view>
				<text class="nav-text">{{ item.text }}</text>
			</view>
		</view>
	</view>
</template>

<script>
import HomeImage from "../static/index/home.svg";
import MedicineImage from "../static/index/medicine.svg"
import HealthImage from "../static/index/health.svg";
import MineImage from "../static/index/mine.png";

export default {
	name: "AppNavbar",
	props: {
		current: {
			type: Number,
			default: 0,
		},
	},
	data() {
		return {
			tabs: [
				{ icon: HomeImage, text: "首页" },
				{ icon: MedicineImage, text: "用药" },
				{ icon: HealthImage, text: "健康" },
				{ icon: MineImage, text: "我的" },
			],
		};
	},
	methods: {
		switchTab(index) {
			// 如果点击的就是当前页，不再重复跳转
			if (this.current === index) return;
			
			this.$emit("change", index);

			const pages = [
				"/pages/index/index", // 假设第一个是首页
				"/pages/ai/index",
				"/pages/health/index",
				"/pages/mine/index",
			];

			// 优先使用 switchTab（对应 pages.json 里的 tabBar 页面）
			uni.switchTab({
				url: pages[index],
				fail: (err) => {
					// 如果不是 tabBar 页面，则使用 reLaunch 或 navigateTo
					uni.reLaunch({
						url: pages[index]
					});
				},
			});
		},
	},
};
</script>

<style lang="scss" scoped>
.tab-bar {
	position: fixed;
	bottom: 40rpx;
	left: 0;
	right: 0;
	padding: 0 40rpx;
	z-index: 999;
	/* 适配底部安全区（针对 iPhone 刘海屏） */
	padding-bottom: constant(safe-area-inset-bottom);
	padding-bottom: env(safe-area-inset-bottom);

	.nav-container {
		background: #1e293b; /* 对应原有的深蓝黑色 */
		border-radius: 65rpx;
		height: 130rpx;
		display: flex;
		justify-content: space-around;
		align-items: center;
		box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.3);
		border: 1rpx solid rgba(255, 255, 255, 0.05);

		@media (prefers-color-scheme: dark) {
			background: #0f172a;
			border: 1rpx solid rgba(255, 255, 255, 0.1);
		}
	}

	.nav-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		flex: 1;
		transition: all 0.3s ease;

		.nav-icon-wrapper {
			width: 60rpx;
			height: 60rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			border-radius: 30rpx;
			transition: all 0.3s ease;

			.nav-icon {
				width: 44rpx;
				height: 44rpx;
				/* 如果是普通的黑白图标，可以增加亮度滤镜来配合深色底 */
				filter: brightness(0) invert(0.8);
			}
		}

		.nav-text {
			font-size: 20rpx;
			color: #94a3b8;
			margin-top: 6rpx;
			font-weight: 500;
		}

		&.active {
			.nav-icon-wrapper {
				background: rgba(99, 102, 241, 0.15); /* 主色背景透明度 */
				width: 100rpx;
				height: 70rpx;
				
				.nav-icon {
					/* 激活状态图标变为主色调 #6366F1 */
					filter: none; 
					// 如果需要通过 CSS 强制改变图标颜色（针对黑白SVG）可以使用 drop-shadow 技巧
				}
			}
			.nav-text {
				color: #6366f1;
				font-weight: bold;
			}
		}

		&:active {
			transform: scale(0.9);
		}
	}
}
</style>