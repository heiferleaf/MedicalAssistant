<template>
	<view class="tab-bar" :class="{ 'dark-mode': isDarkMode }">
		<view class="nav-container">
			<view v-for="(item, index) in tabs.slice(0, 2)" :key="index" class="nav-item"
				:class="{ 'active': current === index }" @tap="switchTab(index)">
				<view class="nav-icon-wrapper">
					<image :src="item.icon" mode="aspectFit" class="nav-icon"></image>
				</view>
			</view>

			<view class="center-spacer"></view>

			<view v-for="(item, index) in tabs.slice(2, 4)" :key="index + 2" class="nav-item"
				:class="{ 'active': current === (index + 2) }" @tap="switchTab(index + 2)">
				<view class="nav-icon-wrapper">
					<image :src="item.icon" mode="aspectFit" class="nav-icon"></image>
				</view>
			</view>
		</view>

		<view class="center-btn-wrapper" @tap="goToAssistant">
			<view class="center-btn">
				<view class="center-btn-inner">
					<image src="/static/Home/ai-assistant.svg" mode="aspectFit" class="icon" style="height: 54rpx;width: 54rpx;" />
				</view>
			</view>
		</view>
	</view>
</template>
<script>
import HomeImage from "../static/index/home.svg";
import MedicineImage from "../static/index/medicine.svg";
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
	mounted() {
		this.applyGlobalSettings();
	},
	data() {
		return {
			globalFontSize: 16, // 默认基准字体大小 16px
			isDarkMode: false,
			tabs: [
				{ icon: HomeImage, text: "首页" },
				{ icon: MedicineImage, text: "用药" },
				{ icon: HealthImage, text: "健康" },
				{ icon: MineImage, text: "我的" },
			],
		};
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
		switchTab(index) {
			if (this.current === index) return;

			this.$emit("change", index);

			// 我根据你的 tab 重新整理了一下路径映射，请确保路径与你项目实际路径一致
			const pages = [
				"/pages/index/Home",       // 0: 首页
				"/pages/index/Health",    // 1: 用药
				"/pages/index/Medicine",      // 2: 健康
				"/pages/index/Mine",        // 3: 我的
			];

			uni.switchTab({
				url: pages[index],
				fail: (err) => {
					uni.reLaunch({
						url: pages[index]
					});
				},
			});
		},

		// 特别按钮的跳转逻辑
		goToAssistant() {
			uni.navigateTo({
				url: "/pages/ai/Assistant",
				fail: (err) => {
					console.error("跳转AI助手页失败:", err);
				}
			});
		}
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
	padding-bottom: constant(safe-area-inset-bottom);
	padding-bottom: env(safe-area-inset-bottom);
	/* 这里的 fixed 定位同时也作为内部按钮 absolute 绝对定位的参考 */
}

.nav-container {
	background: #1e293b;
	border-radius: 48rpx;
	height: 130rpx;
	display: flex;
	justify-content: space-around;
	align-items: center;
	box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.3);
	position: relative;

	/* ====== 【修改：微调挖孔位置】 ====== */
	/* 圆心定在距离顶部 10rpx 的位置，半径 72rpx，边缘更平滑 */
	-webkit-mask: radial-gradient(circle at 50% 10rpx, transparent 72rpx, #000 73rpx);
	mask: radial-gradient(circle at 50% 10rpx, transparent 72rpx, #000 73rpx);
}

/* ====== 【新增：占位符样式】 ====== */
.center-spacer {
	flex: 1;
}

/* 常规导航项 */
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
			opacity: 0.5;
			filter: invert(1);
			transition: all 0.3s ease;
		}
	}

	&.active {
		.nav-icon-wrapper {
			background: rgba(99, 102, 241, 0.8);
			width: 100rpx;
			height: 70rpx;

			.nav-icon {
				opacity: 1;
				filter: invert(1) brightness(200%);
			}
		}
	}

	&:active {
		transform: scale(0.9);
	}
}

/* 正中间特别按钮 */
/* ====== 【修改：中心按钮样式】 ====== */
.center-btn-wrapper {
    /* 使用绝对定位使其悬浮在凹槽上方 */
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    /* 计算公式：挖孔圆心在顶部往下 10rpx。按钮高 120rpx(半径60rpx)。 10 - 60 = -50rpx */
    top: -60rpx; 
    
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000; /* 确保层级最高 */

    .center-btn {
        width: 120rpx;
        height: 120rpx;
        background: transparent;
        border-radius: 50%;
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 10rpx;

        .center-btn-inner {
            width: 100%;
            height: 100%;
            border-radius: 50%;
            background: linear-gradient(135deg, #8b5cf6, #3b82f6);
            box-shadow: 0 10rpx 25rpx rgba(139, 92, 246, 0.5);
            display: flex;
            justify-content: center;
            align-items: center;
            transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);

            &:active {
                transform: scale(0.9);
                box-shadow: 0 5rpx 15rpx rgba(139, 92, 246, 0.4);
            }

            .center-text {
                color: #ffffff;
                font-size: 36rpx;
                font-weight: 900;
                font-style: italic;
                letter-spacing: 2rpx;
            }
        }
    }
}

/* ====================================================
   dark-mode 覆盖样式（亮色背景，暗色图标）
==================================================== */
.dark-mode {
    /* 1. 导航栏背景变亮 */
    .nav-container {
        background: #ffffff; 
        /* 调整阴影，让亮色背景在深色页面上更融合 */
        box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.2); 
    }

    /* 2. 常规图标变暗 */
    .nav-item {
        .nav-icon-wrapper {
            .nav-icon {
                /* 取消反色，恢复原图的暗色（假设您的原图是黑灰色） */
                filter: invert(0); 
                opacity: 0.6;
            }
        }

        /* 3. 选中状态的图标和背景适配 */
        &.active {
            .nav-icon-wrapper {
                /* 选中时的胶囊背景改为浅紫蓝色，保持亮色主题的清爽感 */
                background: rgba(99, 102, 241, 0.15); 
                
                .nav-icon {
                    opacity: 1;
                    /* 保持原色不变暗，取消原先自带的 brightness 增亮 */
                    filter: invert(0); 
                }
            }
        }
    }
}
</style>