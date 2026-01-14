<template>
  <view class="navbar">
    <view class="navbar-content">
      <view
        v-for="(item, index) in tabs"
        :key="index"
        class="nav-item"
        :class="{ active: current === index }"
        @click="switchTab(index)"
      >
        <view class="nav-icon">
          <!-- 使用图片 -->
          <image class="icon" :src="item.icon" mode="widthFix"></image>
        </view>
        <!-- <text class="nav-text">{{ item.text }}</text> -->
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
      this.$emit("change", index);

      const pages = [
        "/pages/ai/index",
        "/pages/health/index",
        "/pages/mine/index",
      ];

      uni.switchTab({
        url: pages[index],
        fail: (err) => {
          uni.reLaunch({ url: pages[index] });
        },
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.navbar {
  width: 100%;
  height: 100rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background: #ffffff;
  border-top: 1rpx solid #f0f0f0;
}

.navbar-content {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 100%;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10rpx 20rpx;

  &.active {
    .nav-text {
      color: #7c3aed;
      font-weight: 500;
    }

    .icon {
      transform: scale(1.667); /* 60/36 ≈ 1.667 */
    }
  }
}

.icon {
  width: 48rpx;
  height: 48rpx;
  background-size: cover;
  background-position: center;
  
  /* 使用 transform 实现从中心缩放 */
  transform-origin: center center; /* 确保从中心点缩放 */
  transition: all 0.5s; /* 添加弹性效果 */
}

.nav-icon {
  margin-bottom: 4rpx;
}

.icon {
  font-size: 40rpx;
  color: #999999;
  transition: color 0.2s;
}

.nav-text {
  font-size: 22rpx;
  color: #666666;
  transition: color 0.2s;
}
</style>
