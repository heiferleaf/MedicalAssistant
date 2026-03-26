<template>
  <view class="section-box">
    <view class="section-title">
      <text class="title-text">快捷工具</text>
    </view>

    <!-- 横向滑动 -->
    <scroll-view scroll-x class="tool-scroll" scrollbar="false">
      <view class="tool-row">
        <view
          v-for="func in functions"
          :key="func.id"
          class="tool-item"
          @tap="handleFunction(func.id)"
        >
          <view class="icon-bg" :style="{ backgroundColor: func.color + '50' }">
            <image
              :src="func.icon"
              mode="aspectFit"
              class="icon"
              style="height: 54rpx; width: 54rpx"
            />
          </view>

          <text class="tool-name">{{ func.name }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import familyApi from "../api/family";
export default {
  data() {
    return {
      functions: [
        {
          id: 1,
          name: "拍照识药",
          icon: "/static/Home/camera.svg",
          color: "#06b6d4",
        },
        {
          id: 2,
          name: "就医清单",
          icon: "/static/Home/medical-list.svg",
          color: "#3539ff",
        },
        {
          id: 3,
          name: "家庭管理",
          icon: "/static/Home/family.svg",
          color: "#10b981",
        },
        // { id: 4, name: "AI 咨询", icon: "/static/Home/ai-assistant.svg", color: "#0ea5e9" },
        {
          id: 4,
          name: "用药提醒",
          icon: "/static/Home/reminder.svg",
          color: "#ef4444",
        },
        // { id: 5, name: "健康信息", icon: "/static/Home/reminder.svg", color: "#8b5cf6" },
        // { id: 6, name: "拍照识别", icon: "/static/Home/reminder.svg", color: "#f59e0b" },
      ],
    };
  },
  methods: {
    handleFunction(funcId) {
      const routes = {
        1: "/pages/scan/DrugScan",
        2: "/pages/medical/Prepare",
        3: "/pages/family/index",
        // 4: "/pages/ai/Assistant",
        4: "/pages/reminder/Reminder",
        // 5: "/pages/test/health",
        // 6: "/pages/test/camera",
      };
      if (funcId === 3) {
        this.checkUserFamilyStatus();
        return;
      }
      if (routes[funcId]) uni.navigateTo({ url: routes[funcId] });
    },
    async checkUserFamilyStatus() {
      if (this.loading) return;
      this.loading = true;
      let res = null;
      try {
        // 1. 获取当前组状态
        res = await familyApi.getMyGroup();
      } catch (e) {
        console.error("获取状态失败", e);
      } finally {
        this.loading = false;
        if (res && res.group) {
          uni.navigateTo({ url: "/pages/family/members" });
        } else {
          uni.navigateTo({ url: "/pages/family/index" });
        }
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.section-box {
  padding: 0 40rpx;
  margin-top: 50rpx;

  .section-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

    .title-text {
      font-size: 34rpx;
      font-weight: bold;
      color: #1e293b;

      @media (prefers-color-scheme: dark) {
        color: #fff;
      }
    }

    .more-link {
      font-size: 26rpx;
      color: #94a3b8;
      display: flex;
      align-items: center;
    }
  }
}

/* 快捷工具网格 */
/* 横向滚动 */
.tool-scroll {
  width: 100%;
}

/* 横向排列 */
.tool-row {
  display: flex;
}

/* 每个工具 */
.tool-item {
  width: 25%;
  flex-shrink: 0;

  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;

  .icon-bg {
    width: 120rpx;
    height: 120rpx;
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .tool-name {
    font-size: 24rpx;
    color: #64748b;
    font-weight: 500;
  }

  &:active {
    transform: scale(0.92);
  }
}

/* 图标库占位 */
.iconfont {
  font-family: "Material Symbols Outlined";
  font-size: 48rpx;
}
</style>