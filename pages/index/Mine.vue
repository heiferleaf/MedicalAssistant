<template>
  <view class="page-container">
    <view class="header-section">
      <view class="status-bar-placeholder"></view>
      <view class="padding"></view>

      <view class="header-content">
        <view class="user-info">
          <view class="avatar-container">
            <image class="avatar" :src="getAvatar()" mode="aspectFill"></image>
            <view class="badge-dot"></view>
          </view>

          <view class="user-details">
            <view class="user-name-box">
              <text class="user-name">{{ username }}</text>
              <image
                @click="toProfile()"
                class="icon"
                src="/static/Mine/right-arrow-white.svg"
              ></image>
            </view>
            <view class="user-tags">
              <view class="tag">
                <text>Lv {{ getUserLevel() }}</text>
              </view>
              <view class="tag">
                <text>成为用户第{{ createTime ? Math.floor((Date.now() - new Date(createTime).getTime()) / (1000 * 60 * 60 * 24)) : 0 }}天</text>
              </view>
            </view>
          </view>
        </view>

        <view class="settings-btn" @click="toSettings()">
          <image class="icon" src="/static/Mine/setting.svg"></image>
        </view>
      </view>

      <view class="floating-card glass-effect">
        <view class="health-data">
          <view class="data-item">
            <view class="data-value"
              >120/80 <text class="data-unit">mmHg</text></view
            >
            <view class="data-label">血压</view>
          </view>
          <view class="divider"></view>
          <view class="data-item">
            <view class="data-value"
              >72 <text class="data-unit">bpm</text></view
            >
            <view class="data-label">心率</view>
          </view>
          <view class="divider"></view>
          <view class="data-item">
            <view class="data-value"
              >5.2 <text class="data-unit">mmol/L</text></view
            >
            <view class="data-label flex-center">
              血糖
            </view>
          </view>
        </view>
      </view>
    </view>

    <scroll-view class="main-content" scroll-y>
      <view class="section-header">
        <text class="section-title">常用功能</text>
      </view>

      <view class="menu-list">
        <view class="menu-item hover-effect" @click="navigateTo('history')">
          <view class="menu-icon-box bg-indigo">
            <image class="icon" src="../../static/Health/clock-history.svg" />
          </view>
          <view class="menu-info">
            <text class="menu-title">服药记录</text>
            <text class="menu-desc">查看用药任务历史</text>
          </view>
          <view class="menu-action">
            <!-- <text class="badge-num">3</text> -->
            <image class="icon" src="/static/Mine/right-arrow.svg"></image>
          </view>
        </view>

        <view class="menu-item hover-effect" @click="navigateTo('family')">
          <view class="menu-icon-box bg-emerald">
            <image class="icon" src="../../static/Home/family.svg" />
          </view>
          <view class="menu-info">
            <text class="menu-title">家庭成员管理</text>
            <text class="menu-desc">查看家人健康数据</text>
          </view>
          <view class="menu-action">
            <image class="icon" src="/static/Mine/right-arrow.svg"></image>
          </view>
        </view>

        <view class="menu-item hover-effect" @click="navigateTo('camera')">
          <view class="menu-icon-box bg-blue">
            <image class="icon" src="../../static/Home/camera.svg" />
          </view>
          <view class="menu-info">
            <text class="menu-title">拍照识别</text>
            <text class="menu-desc">拍照提取药物注意事项</text>
          </view>
          <view class="menu-action">
            <image class="icon" src="/static/Mine/right-arrow.svg"></image>
          </view>
        </view>

        <view class="menu-item hover-effect" @click="navigateTo('prepare')">
          <view class="menu-icon-box bg-amber">
            <image class="icon" src="../../static/Mine/medical-record.svg" />
          </view>
          <view class="menu-info">
            <text class="menu-title">就医准备单</text>
            <text class="menu-desc">生成一份就医准备单</text>
          </view>
          <view class="menu-action">
            <!-- <text class="badge-num">5</text> -->
            <image class="icon" src="/static/Mine/right-arrow.svg"></image>
          </view>
        </view>
      </view>

            <!-- 退出登录 -->
      <view class="logout-section">
        <button class="logout-btn" @click="logout">
          <text class="logout-text">退出登录</text>
        </button>
      </view>
    </scroll-view>
  </view>
</template>

<script>

export default {
  data() {
    return {
      username: "小明",
      createTime: "",
    };
  },
  mounted() {
    this.initData();
  },
  methods: {
    getUserLevel() {
      // 简单的等级计算逻辑：每满30天增加一级，最高LV.10
      if (!this.createTime) return 1;
      const days = Math.floor(
        (Date.now() - new Date(this.createTime).getTime()) /
          (1000 * 60 * 60 * 24)
      );
      return Math.min(10, Math.floor(days / 30) + 1);
    },
    initData() {
      const username = uni.getStorageSync("username") || "小明";
      this.username = username;
      this.createTime = uni.getStorageSync("createTime") || "";
    },
    getAvatar() {
      const id = uni.getStorageSync("userId") || 1;
      // 用 userId 做简单哈希，映射到 1-100
      const index = (Math.abs(Number(id)) % 100) + 1;
      return `http://8.148.94.242/avatar/file/avatar_${index}.svg`;
    },
    toSettings() {
      uni.navigateTo({ url: "/pages/mine/Settings" });
    },
    toProfile() {
      uni.navigateTo({ url: "/pages/mine/profile" });
    },
    navigateTo(type) {
      console.log("跳转至：", type);
      switch (type) {
        case "history":
          uni.navigateTo({ url: `/pages/reminder/Reminder?tab=history` });
          break;
        case "family":
          uni.navigateTo({ url: "/pages/family/index" });
          break;
        case "report":
          uni.navigateTo({ url: "/pages/mine/report" });
          break;
        case "prepare":
          uni.navigateTo({ url: "/pages/medical/Prepare" });
          break;
        case "camera":
          uni.navigateTo({ url: "/pages/scan/DrugScan" });
          break;
        case "AI":
          uni.navigateTo({ url: "/pages/ai/Assistant" });
          break;
        default:
          break;
      }
    },
    logout() {
      uni.showModal({
        title: "确认退出",
        content: "您确定要退出登录吗？",
        success: (res) => {
          if (res.confirm) {
            uni.clearStorageSync(); // 清除 userId 和双 Token
            uni.reLaunch({ url: "/pages/Login" });
          }
        },
        showCancel: true,
        cancelText: "取消",
        confirmText: "退出",
      });
    },
  },
};
</script>

<style lang="scss" scoped>
$primary: #4d88ff;
$bg-light: #f8fafc;
$bg-dark: #0f172a;
$dark-blue: #3756e8;

.padding {
  height: 64rpx;
  background: transparent;
  /* 顶部留白，适配状态栏 */
}

.mine-container {
  min-height: 100vh;
  background-color: $bg-light;

  @media (prefers-color-scheme: dark) {
    background-color: $bg-dark;
  }
}

.status-bar {
  height: var(--status-bar-height);
}

.header-section {
  height: 500rpx;
  // position: relative;
  /* 假设 top-row 已经有它自己的高度和 padding */
  // padding-bottom: 80rpx; /* 给 header 内部底部留出空间 */
  margin-bottom: 80rpx; /* 给 header 外部留出空间，防止挡住下面的页面内容 */
  z-index: 1;
  .top-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    // margin-bottom: 50rpx;
    width: 100%;
    position: relative;
    z-index: 2;

    .page-title {
      font-size: 48rpx;
      font-weight: 700;
      color: #1e293b;

      @media (prefers-color-scheme: dark) {
        color: #fff;
      }
    }

    .settings-btn {
      width: 80rpx;
      height: 80rpx;
      background: transparent;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);

      @media (prefers-color-scheme: dark) {
        background: #1e293b;
      }

      .material-icons {
        color: #64748b;
        font-size: 40rpx;
      }
      .icon {
        width: 40rpx;
        height: 40rpx;
      }
    }
  }
}

.user-card {
  background: linear-gradient(135deg, #2861eb 50%, #4b49e6 100%);
  padding: 20rpx;
  border-radius: 0 0 100rpx 100rpx;
  display: flex;
  align-items: center;
  gap: 30rpx;
  // border: 1rpx solid #f1f5f9;
  box-sizing: border-box;
  width: 100%;
  height: 400rpx;
  z-index: 10;

  @media (prefers-color-scheme: dark) {
    background: #1e293b;
    border-color: rgba(255, 255, 255, 0.05);
  }

  .avatar-wrapper {
    position: relative;
    width: 120rpx;
    height: 120rpx;
    border-radius: 60rpx;

    border: #4d75ed 4rpx solid;
    display: flex;
    align-items: center;
    justify-content: center;

    .avatar {
      width: 90rpx;
      height: 90rpx;
      border-radius: 45rpx;
      background: #ffedd5;
      border: 6rpx solid $bg-light;

      @media (prefers-color-scheme: dark) {
        border-color: #334155;
      }
    }

    .online-status {
      position: absolute;
      bottom: 6rpx;
      right: 6rpx;
      width: 20rpx;
      height: 20rpx;
      background: #10b981;
      border: 4rpx solid #fff;
      border-radius: 50%;

      @media (prefers-color-scheme: dark) {
        border-color: #1e293b;
      }
    }
  }

  .user-info {
    flex: 1;
    padding-top: 32rpx;

    .user-name {
      font-size: 40rpx;
      font-weight: 700;
      color: #ffffff;
      display: block;

      @media (prefers-color-scheme: dark) {
        color: #fff;
      }
    }

    .user-status {
      font-size: 26rpx;
      color: #64748b;
      margin-top: 4rpx;
      display: block;
    }

    .badge {
      display: inline-block;
      background: rgba(255, 255, 255, 0.2);
      padding: 4rpx 16rpx;
      border-radius: 20rpx;
      margin-top: 12rpx;
      height: 40rpx;

      align-items: center;
      justify-content: center;

      .badge-text {
        color: #ffffff;
        font-size: 4rpx;
        font-weight: 400;
      }
    }
  }

  .arrow {
    color: #cbd5e1;
  }
}

.stats-section {
  position: absolute;
  left: 50%;

  /* 🌟 关键修改 1：让它的底边对齐蓝色背景的底边 */
  bottom: 0;

  /* 🌟 关键修改 2：X轴居中，Y轴往下移动自身高度的 50% */
  transform: translate(-50%, 50%);

  width: 92%;
  z-index: 100;

  /* 毛玻璃效果核心 */
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20rpx);
  -webkit-backdrop-filter: blur(20rpx);

  /* 外观质感与圆角 */
  border-radius: 32rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 12rpx 32rpx rgba(0, 0, 0, 0.05);

  /* 内部数据排版 */
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 36rpx 20rpx;
  box-sizing: border-box;

  .stat-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12rpx;

    .stat-label {
      font-size: 26rpx;
      color: #666666;
    }

    .stat-value {
      font-size: 40rpx;
      font-weight: bold;
    }
  }
}

// 统计配色
.bg-indigo {
  background: #eef2ff;

  @media (prefers-color-scheme: dark) {
    background: rgba(99, 102, 241, 0.1);
  }
}

.color-indigo {
  color: #4f46e5;
}

.bg-emerald {
  background: #ecfdf5;

  @media (prefers-color-scheme: dark) {
    background: rgba(16, 185, 129, 0.1);
  }
}

.color-emerald {
  color: #059669;
}

.bg-rose {
  background: #fff1f2;

  @media (prefers-color-scheme: dark) {
    background: rgba(244, 63, 94, 0.1);
  }
}

.color-rose {
  color: #e11d48;
}

.menu-section {
  padding: 40rpx;

  .menu-group {
    background: #fff;
    border-radius: 50rpx;
    overflow: hidden;
    border: 1rpx solid #f1f5f9;

    @media (prefers-color-scheme: dark) {
      background: #1e293b;
      border-color: rgba(255, 255, 255, 0.05);
    }
  }

  .menu-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 36rpx 40rpx;
    transition: background 0.2s;

    .menu-left {
      display: flex;
      align-items: center;
      gap: 30rpx;

      .menu-text {
        font-size: 30rpx;
        font-weight: 500;
        color: #1e293b;

        @media (prefers-color-scheme: dark) {
          color: #f1f5f9;
        }
      }
    }

    .menu-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;

      .material-icons {
        font-size: 40rpx;
      }
    }

    .arrow-sm {
      color: #cbd5e1;
      font-size: 40rpx;
    }
  }

  .divider {
    height: 1rpx;
    background: #f1f5f9;
    margin: 0 40rpx;

    @media (prefers-color-scheme: dark) {
      background: rgba(255, 255, 255, 0.05);
    }
  }
}

// 菜单配色
.bg-blue-light {
  background: #eff6ff;

  @media (prefers-color-scheme: dark) {
    background: rgba(59, 130, 246, 0.1);
  }
}

.color-blue {
  color: #3b82f6;
}

.bg-purple-light {
  background: #faf5ff;

  @media (prefers-color-scheme: dark) {
    background: rgba(168, 85, 247, 0.1);
  }
}

.color-purple {
  color: #a855f7;
}

.bg-orange-light {
  background: #fff7ed;

  @media (prefers-color-scheme: dark) {
    background: rgba(249, 115, 22, 0.1);
  }
}

.color-orange {
  color: #f97316;
}

.bg-slate-light {
  background: #f8fafc;

  @media (prefers-color-scheme: dark) {
    background: #0f172a;
  }
}

.color-slate {
  color: #64748b;
}

.hover-gray {
  background-color: #f8fafc !important;

  @media (prefers-color-scheme: dark) {
    background-color: #334155 !important;
  }
}

.mt-4 {
  margin-top: 32rpx;
}

.safe-area-bottom {
  height: 240rpx;
}

.material-icons {
  font-family: "Material Symbols Outlined";
}

.btn-manage {
  background-color: $primary;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  width: 80%;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 32rpx;
  box-shadow: 0 8rpx 16rpx rgba($primary, 0.2);

  &::after {
    border: none;
  }
}

.btn-manage-hover {
  background-color: #3b76eb;
}

.scroll-body {
  height: calc(100vh - 120rpx);
}
/* 引入外部图标库 (App 端强烈建议下载到本地引用，这里仅做演示) */
@import url("https://fonts.googleapis.com/icon?family=Material+Icons");

/* 颜色变量定义 */
:root {
  --primary-color: #5e5ce6;
  --bg-color: #f8f9ff;
  --text-main: #1e293b;
  --text-sub: #94a3b8;
}

.page-container {
  min-height: 100vh;
  background-color: var(--bg-color, #f8f9ff);
  padding-bottom: 160rpx; /* 给底部导航留出空间 */
  box-sizing: border-box;
}

/* --- 1. 头部区域 --- */
.header-section {
  position: relative;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  border-bottom-left-radius: 60rpx;
  border-bottom-right-radius: 60rpx;
  padding: 0 48rpx 120rpx 48rpx;

  height: 300rpx;
}

.status-bar-placeholder {
  height: var(--status-bar-height); /* APP端自动获取系统状态栏高度 */
  width: 100%;
  padding-top: 40rpx;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-top: 20rpx;
}

.user-info {
  display: flex;
  align-items: center;
}

.avatar-container {
  position: relative;
  margin-right: 32rpx;
}

.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  padding: 4rpx;
  box-sizing: border-box;
}

.badge-dot {
  position: absolute;
  top: 0;
  right: 0;
  width: 24rpx;
  height: 24rpx;
  background-color: #ef4444;
  border: 4rpx solid #fff;
  border-radius: 50%;
}

.user-name-box {
  display: flex;
  align-items: center;
  color: #fff;
}

.user-name {
  font-size: 48rpx;
  font-weight: bold;
}

.icon-right {
  font-size: 32rpx;
  opacity: 0.7;
  margin-left: 8rpx;
}

.user-tags {
  display: flex;
  margin-top: 16rpx;
  flex-wrap: wrap;
}

.tag {
  background-color: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  margin-right: 16rpx;
}

.tag-icon {
  font-size: 20rpx;
  margin-right: 8rpx;
}

.settings-btn {
  color: rgba(255, 255, 255, 0.8);
  font-size: 48rpx;
}

/* 悬浮数据卡片 */
.floating-card {
  position: absolute;
  bottom: -100rpx;
  left: 48rpx;
  right: 48rpx;
  background: rgba(255, 255, 255, 0.15);
  border: 2rpx solid rgba(255, 255, 255, 0.2);
  border-radius: 40rpx;
  padding: 40rpx;
  box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.1);
}

.glass-effect {
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.health-data {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}

.data-item {
  flex: 1;
  text-align: center;
}

.data-value {
  font-size: 28rpx;
  font-weight: bold;
  line-height: 1;
}

.data-unit {
  font-size: 16rpx;
  font-weight: normal;
  opacity: 0.7;
}

.data-label {
  font-size: 28rpx;
  font-weight: 600;
  opacity: 0.7;
  margin-top: 8rpx;
  color: #4b49e6;
}

.flex-center {
  display: flex;
  justify-content: center;
  align-items: center;
}

.trend-icon {
  font-size: 20rpx;
  margin-left: 8rpx;
}

.divider {
  width: 2rpx;
  height: 64rpx;
  background-color: rgba(255, 255, 255, 0.2);
}

/* --- 2. 主体功能区 --- */
.main-content {
  margin-top: 60rpx;
  padding: 0 48rpx;
  width: 100%;
  height: calc(100vh - 800rpx);
  /* 加上这行，解决水平撑爆的问题 */
  box-sizing: border-box;

  margin-bottom: 240rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 36rpx;
  font-weight: bold;
  color: var(--text-main);
}

.section-more {
  font-size: 24rpx;
  color: var(--text-sub);
}

.menu-list {
  background-color: rgba(241, 245, 249, 0.5); /* slate-50 */
  border-radius: 40rpx;
  padding: 16rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  background-color: #fff;
  padding: 32rpx;
  border-radius: 32rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.02);
  transition: all 0.2s;
}

.menu-item:last-child {
  margin-bottom: 0;
}

.hover-effect:active {
  transform: scale(0.98);
  opacity: 0.9;
}

.menu-icon-box {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 32rpx;
}

/* 图标颜色定义 */
.bg-indigo {
  background-color: #eef2ff;
}
.color-indigo {
  color: var(--primary-color, #5e5ce6);
}
.bg-emerald {
  background-color: #ecfdf5;
}
.color-emerald {
  color: #10b981;
}
.bg-blue {
  background-color: #eff6ff;
}
.color-blue {
  color: #3b82f6;
}
.bg-amber {
  background-color: #fffbeb;
}
.color-amber {
  color: #f59e0b;
}

.menu-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.menu-title {
  font-size: 30rpx;
  font-weight: bold;
  color: var(--text-main);
}

.menu-desc {
  font-size: 24rpx;
  color: var(--text-sub);
  margin-top: 4rpx;
}

.menu-action {
  display: flex;
  align-items: center;
}

.badge-num {
  background-color: #ef4444;
  color: #fff;
  font-size: 20rpx;
  font-weight: bold;
  padding: 4rpx 12rpx;
  border-radius: 20rpx;
  margin-right: 16rpx;
}

.arrow-icon {
  color: #cbd5e1;
  font-size: 40rpx;
}

/* --- 3. AI Banner --- */
.ai-banner {
  margin-top: 40rpx;
  position: relative;
  overflow: hidden;
  background-color: rgba(94, 92, 230, 0.1);
  padding: 40rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
}

.ai-info {
  flex: 1;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.ai-title {
  font-size: 32rpx;
  font-weight: bold;
  color: var(--primary-color, #5e5ce6);
}

.ai-desc {
  font-size: 22rpx;
  color: #64748b;
  margin-top: 8rpx;
}

.ai-btn {
  margin-top: 24rpx;
  background-color: var(--primary-color, #5e5ce6);
  color: #fff;
  font-size: 22rpx;
  padding: 0 24rpx;
  height: 48rpx;
  line-height: 48rpx;
  border-radius: 24rpx;
  margin-left: 0; /* 重置 button 默认居中 */
}

.button-hover {
  opacity: 0.8;
}

.ai-icon-bg {
  width: 160rpx;
  height: 160rpx;
  opacity: 0.2;
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  right: 20rpx;
}

.ai-icon-large {
  font-size: 120rpx;
  color: var(--primary-color, #5e5ce6);
}

/* --- 4. 底部自定义导航 (仅做UI展示) --- */
.custom-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(255, 255, 255, 0.9);
  border-top: 2rpx solid #f1f5f9;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding-top: 16rpx;
  z-index: 99;
}

.safe-area-bottom {
  padding-bottom: calc(16rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #94a3b8;
}

.tab-item.active {
  color: var(--primary-color, #5e5ce6);
}

.tab-icon {
  font-size: 48rpx;
}

.tab-text {
  font-size: 20rpx;
  margin-top: 4rpx;
}

/* 退出登录 */
.logout-section {
  margin-top: 48rpx;
  margin-bottom: 32rpx;
}

.logout-btn {
  width: 80%;
  height: 100rpx;
  padding: auto;
  background: linear-gradient(135deg, #2d6bff 0%, #2d6bff 100%);
  border: 2rpx solid rgba(77, 142, 255, 0.2);
  border-radius: 50rpx;
  box-shadow: 0 4rpx 16rpx rgba(45, 107, 255, 0.1);
  transition: all 0.5s ease;

  display: flex;
  align-items: center;
  justify-content: center;
}

.logout-text {
  font-size: 32rpx;
  font-weight: bold;
  color: #fff;
}
</style>