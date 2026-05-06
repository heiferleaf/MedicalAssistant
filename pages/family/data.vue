<template>
  <view class="page-container">
    <view class="padding"></view>
    <view class="status-bar-padding"></view>

    <view class="header">
      <view class="icon-btn" @click="goBack">
        <image
          class="icon"
          src="/static/Register/back.png"
          mode="aspectFit"
        ></image>
      </view>
      <text class="header-title"
        >{{
          healthData.nickname ? healthData.nickname + "的" : ""
        }}健康看板</text
      >
      <view class="icon-btn" @click="goNotifications">
        <image
          class="icon"
          src="/static/Home/warning.svg"
          mode="aspectFit"
        ></image>
      </view>
    </view>

    <scroll-view scroll-y class="main-content">
      <view class="section">
        <text class="section-title">今日任务概览</text>
        <view class="task-card">
          <view class="task-info">
            <view>
              <text class="task-title">健康与用药打卡</text>
              <text class="task-desc">
                已完成 {{ healthData.completedTasks || 0 }} /
                {{ healthData.totalTasks || 0 }} 项
              </text>
            </view>
            <view class="task-percent">
              任务进度{{ progressPercent || "--" }}%
            </view>
          </view>
          <view class="progress-track">
            <view
              class="progress-fill"
              :style="{ width: progressPercent + '%' }"
            ></view>
          </view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">生理指标</text>
        <view class="grid-container">
          <view class="indicator-card">
            <view class="card-header">
              <view class="icon-box bg-red-light">
                <image
                  class="icon-sm"
                  src="/static/Home/heart.svg"
                  mode="aspectFit"
                ></image>
              </view>
            </view>
            <text class="card-label">心率</text>
            <view class="card-value-box" v-if="healthData.heartRate">
              <text class="value">{{ healthData.heartRate }}</text>
              <text class="unit">bpm</text>
            </view>
            <view class="card-value-box empty" v-else>
              <text class="empty-text">暂无记录</text>
            </view>
          </view>

          <view class="indicator-card">
            <view class="card-header">
              <view class="icon-box bg-blue-light">
                <image
                  class="icon-sm"
                  src="/static/Health/relax.svg"
                  mode="aspectFit"
                ></image>
              </view>
            </view>
            <text class="card-label">放松</text>
            <view class="card-value-box" v-if="healthData.relaxDuration">
              <text class="value">{{ healthData.relaxDuration }}</text>
              <text class="unit">分钟</text>
            </view>
            <view class="card-value-box empty" v-else>
              <text class="empty-text">暂无记录</text>
            </view>
          </view>

          <view class="indicator-card">
            <view class="card-header">
              <view class="icon-box bg-cyan-light">
                <image
                  class="icon-sm"
                  src="/static/Health/blood_oxygen.svg"
                  mode="aspectFit"
                ></image>
              </view>
            </view>
            <text class="card-label">血氧</text>
            <view class="card-value-box" v-if="healthData.bloodOxygen">
              <text class="value">{{ healthData.bloodOxygen }}</text>
              <text class="unit">%</text>
            </view>
            <view class="card-value-box empty" v-else>
              <text class="empty-text">暂无记录</text>
            </view>
          </view>

          <view class="indicator-card">
            <view class="card-header">
              <view class="icon-box bg-blue-light">
                <image
                  class="icon-sm"
                  src="/static/Health/pressure.svg"
                  mode="aspectFit"
                ></image>
              </view>
            </view>
            <text class="card-label">压力</text>
            <view class="card-value-box" v-if="healthData.pressureAvgScore">
              <text class="value">{{ healthData.pressureAvgScore }}</text>
              <text class="unit">分</text>
            </view>
            <view class="card-value-box empty" v-else>
              <text class="empty-text">暂无记录</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">日常状态</text>
        <view class="list-container">
          <view class="list-card">
            <view class="list-left">
              <view class="icon-box bg-indigo-light">
                <image
                  class="icon-sm"
                  src="/static/Prepare/sleep.svg"
                  mode="aspectFit"
                ></image>
              </view>
              <view class="list-text-group">
                <text class="list-title">昨晚睡眠</text>
                <text class="list-desc" v-if="healthData.sleepDuration">{{
                  healthData.sleepDuration
                }}</text>
                <text class="list-desc empty" v-else>尚未同步</text>
              </view>
            </view>
            <view class="list-right">
              <text class="status-text" v-if="healthData.sleepScore"
                >效率 {{ healthData.sleepScore }}%</text
              >
            </view>
          </view>

          <view class="list-card">
            <view class="list-left">
              <view class="icon-box bg-orange-light">
                <image
                  class="icon-sm"
                  src="/static/Prepare/walk.svg"
                  mode="aspectFit"
                ></image>
              </view>
              <view class="list-text-group">
                <text class="list-title">今日步数</text>
                <text class="list-desc" v-if="healthData.stepCount"
                  >{{ healthData.stepCount }} 步</text
                >
                <text class="list-desc empty" v-else>尚未同步</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-spacer"></view>
    </scroll-view>
  </view>
</template>

<script>
// 路由与交互方法
export default {
  data() {
    return {
      healthData: null,
    };
  },
  onLoad(options) {
    // 页面加载时的初始化逻辑
    if (options.healthData) {
      const decodedStr = decodeURIComponent(options.healthData);
      this.healthData = JSON.parse(decodedStr);
      console.log(
        "接收到的健康数据: ",
        JSON.parse(JSON.stringify(this.healthData))
      );
    }
  },
  methods: {
    goBack() {
      uni.navigateBack({
        delta: 1,
        fail: () => uni.switchTab({ url: "/pages/index/index" }),
      });
    },

    goNotifications() {
      uni.showToast({ title: "消息通知", icon: "none" });
    },

    addRecord() {
      uni.showToast({ title: "添加药品记录", icon: "none" });
    },

    // 底部导航切换
    switchTab(tabName) {
      console.log("切换到:", tabName);
      // uni.switchTab({ url: `/pages/${tabName}/index` });
    },
  },
};
</script>

<style lang="scss" scoped>
.padding {
  height: 64rpx;
  /* 顶部留白，适配状态栏 */
}

/* 全局变量 */
$primary: #4d88ff;
$bg-color: #f5f6f8;
$card-bg: #ffffff;
$text-main: #0f172a;
$text-sub: #64748b;
$border-color: #f1f5f9;

/* 通用图标尺寸 */
.icon {
  width: 48rpx;
  height: 48rpx;
  flex-shrink: 0;
}

.icon-sm {
  width: 36rpx;
  height: 36rpx;
  flex-shrink: 0;
}
.icon-xs {
  width: 24rpx;
  height: 24rpx;
  opacity: 0.5;
}

.icon-huge {
  width: 120rpx;
  height: 120rpx;
  flex-shrink: 0;
  opacity: 0.4;
}

.icon-tab {
  width: 48rpx;
  height: 48rpx;
  flex-shrink: 0;
  margin-bottom: 8rpx;
}

.mr-1 {
  margin-right: 8rpx;
}
.padding {
  height: 64rpx; /* 顶部留白，适配状态栏 */
}
.page-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: $bg-color;
  font-family: "Inter", sans-serif;
}

/* 顶部导航 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  background-color: $card-bg;
  border-bottom: 2rpx solid $border-color;
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f5f6f8;

  .icon-btn {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;

    &:active {
      background-color: #f8fafc;
    }
  }

  .header-title {
    font-size: 36rpx;
    font-weight: bold;
    color: $text-main;
  }
}

/* 主内容区 */
.main-content {
  flex: 1;
  height: 0;
}

.section {
  padding: 32rpx;

  .section-title {
    font-size: 40rpx;
    font-weight: bold;
    color: $text-main;
    display: block;
    margin-bottom: 32rpx;
  }
}

/* 药品空状态卡片 */
.empty-card {
  background-color: $card-bg;
  border-radius: 32rpx;
  padding: 64rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2rpx solid $border-color;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.02);

  .empty-icon-wrapper {
    width: 320rpx;
    height: 200rpx;
    background-color: rgba($primary, 0.05);
    border-radius: 24rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 48rpx;
  }

  .empty-title {
    font-size: 36rpx;
    font-weight: bold;
    color: $text-main;
    margin-bottom: 12rpx;
  }

  .empty-desc {
    font-size: 28rpx;
    color: $text-sub;
    text-align: center;
    margin-bottom: 48rpx;
  }

  .btn-add {
    background-color: $primary;
    color: #ffffff;
    font-size: 28rpx;
    font-weight: 600;
    height: 80rpx;
    padding: 0 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 999rpx;

    &::after {
      border: none;
    }
  }

  .btn-add-hover {
    background-color: #3b76eb;
  }
}

/* 生理指标 2x2 网格 */
.grid-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 定义两列，每列平分宽度 */
  gap: 24rpx; /* 卡片之间的上下左右间距 */
}

.indicator-card {
  flex: 1;
  background-color: $card-bg;
  border-radius: 24rpx;
  padding: 32rpx;
  border: 2rpx solid $border-color;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.02);

  &:active {
    background-color: #f8fafc;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24rpx;

    .icon-box {
      width: 64rpx;
      height: 64rpx;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .bg-red-light {
      background-color: #fee2e2;
    }

    .bg-blue-light {
      background-color: #e0e7ff;
    }
    .bg-cyan-light {
      background: #ecfeff;
      @media (prefers-color-scheme: dark) {
        background: rgba(6, 182, 212, 0.1);
      }
    }
    .bg-indigo-light {
      background: #0037eb;
      @media (prefers-color-scheme: dark) {
        background: rgba(9, 13, 225, 0.1);
      }
    }
    .bg-orange-light {
      background: #fff7ed;
      @media (prefers-color-scheme: dark) {
        background: rgba(249, 115, 22, 0.1);
      }
    }

    .chevron {
      opacity: 0.4;
    }
  }

  .card-label {
    font-size: 24rpx;
    font-weight: 500;
    color: $text-sub;
    margin-bottom: 8rpx;
    display: block;
  }

  .card-value-box {
    display: flex;
    align-items: baseline;
    gap: 8rpx;

    .value {
      font-size: 48rpx;
      font-weight: bold;
      color: $text-main;
    }

    .unit {
      font-size: 24rpx;
      color: $text-sub;
    }
  }

  .card-status {
    margin-top: 24rpx;
    display: flex;
    align-items: center;
    gap: 8rpx;

    .dot {
      width: 16rpx;
      height: 16rpx;
      border-radius: 50%;
    }

    .bg-green {
      background-color: #22c55e;
    }

    .status-text {
      font-size: 20rpx;
      font-weight: 500;
    }

    .text-green {
      color: #22c55e;
    }
  }
}

.bottom-spacer {
  height: 160rpx;
}

/* 底部导航栏 */
.custom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-top: 2rpx solid $border-color;
  padding: 16rpx 32rpx 8rpx;
  z-index: 20;

  &.pb-safe {
    padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  }

  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .tab-text {
      font-size: 20rpx;
      font-weight: 500;
      color: $text-sub;
      margin-top: 4rpx;
    }

    &.active {
      .tab-text {
        font-weight: bold;
        color: $primary;
      }
    }
  }
}

/* 日常状态列表容器 */
.list-container {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 10rpx 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
}

/* 列表单行卡片 */
.list-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
}
.list-card:last-child {
  border-bottom: none;
}

/* 列表左侧图文 */
.list-left {
  display: flex;
  align-items: center;

  .icon-box {
    width: 64rpx;
    height: 64rpx;
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .bg-indigo-light {
    background: #0037eb;
    @media (prefers-color-scheme: dark) {
      background: rgba(9, 13, 225, 0.1);
    }
  }
  .bg-orange-light {
    background: #fff7ed;
    @media (prefers-color-scheme: dark) {
      background: rgba(249, 115, 22, 0.1);
    }
  }
}
.list-text-group {
  display: flex;
  flex-direction: column;
  margin-left: 24rpx;
}
.list-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 8rpx;
}
.list-desc {
  font-size: 32rpx;
  color: #111;
  font-weight: bold;
}
.list-desc.empty {
  font-size: 24rpx;
  color: #999;
  font-weight: normal;
}

/* 列表右侧状态 */
.list-right {
  display: flex;
  align-items: center;
}
.status-text {
  font-size: 24rpx;
  color: #666;
  margin-right: 12rpx;
}
</style>
