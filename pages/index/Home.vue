<template>
  <view class="page-container">
    <view class="status-bar"></view>

    <view class="header-section">
      <view class="user-row">
        <view class="user-info" @click="toProfile">
          <image
            class="avatar-img"
            :src="userInfo.avatar || '/static/avatars/avatar1.svg'"
            mode="aspectFill"
          />
          <view class="welcome-text">
            <text class="greeting">你好，</text>
            <text class="user-name">{{ userInfo.name }}</text>
          </view>
        </view>
        <view class="header-btns">
          <view class="icon-btn" @click="toNotification">
            <image src="/static/Home/bell.svg" class="btn-icon" />
            <view class="dot" v-if="hasNotification"></view>
          </view>
        </view>
      </view>

      <view class="date-card">
        <view class="date-left">
          <text class="big-day">{{ currentDay }}</text>
          <view class="month-year">
            <text>{{ currentMonth }}月</text>
            <text>{{ currentYear }}</text>
          </view>
        </view>
        <view class="date-right">
          <text class="week-tag">{{ currentWeek }}</text>
        </view>
      </view>
    </view>

    <scroll-view
      class="main-content"
      scroll-y
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="section-container">
        <view class="section-header">
          <text class="title">今日服药任务</text>
          <view class="more-link" @click="toMedicationList">
            <text>任务中心</text>
            <image src="/static/Home/right-arrow.svg" class="arrow-icon" />
          </view>
        </view>

        <view class="task-list">
          <view
            v-for="item in medicationList"
            :key="item.id"
            class="task-card"
            :class="getStatusClass(item.status)"
          >
            <view class="task-time">
              <text class="time">{{ item.timePoint }}</text>
              <view class="status-indicator"></view>
            </view>
            <view class="task-info">
              <text class="medicine-name">{{ item.medicineName }}</text>
              <text class="dosage">{{ item.dosage }}</text>
            </view>
            <view class="task-action">
              <button
                v-if="item.status === 0"
                class="btn-confirm"
                @click="handleTakeMedicine(item)"
              >
                服用
              </button>
              <view v-else-if="item.status === 1" class="status-label done">
                <text class="icon">✓</text>已服
              </view>
              <view v-else-if="item.status === 2" class="status-label missed">
                <text class="icon">!</text>漏服
              </view>
            </view>
          </view>

          <view v-if="medicationList.length === 0" class="empty-state">
            今日暂无用药任务
          </view>
        </view>
      </view>

      <view class="section-container">
        <view class="section-header">
          <text class="title">快捷工具</text>
        </view>
        <view class="function-grid">
          <view
            v-for="func in functions"
            :key="func.id"
            class="func-item"
            @click="handleFunction(func.id)"
          >
            <view class="func-icon-bg" :style="{ backgroundColor: func.color }">
              <image :src="func.icon" class="func-icon" />
            </view>
            <text class="func-name">{{ func.name }}</text>
          </view>
        </view>
      </view>

      <view class="section-container">
        <view class="section-header">
          <text class="title">今日健康指标</text>
        </view>
        <view class="health-row">
          <view class="health-card">
            <text class="label">今日步数</text>
            <view class="val-box">
              <text class="value">{{ healthData.steps }}</text>
              <text class="unit">步</text>
            </view>
          </view>
          <view class="health-card">
            <text class="label">心率状态</text>
            <view class="val-box">
              <text class="value">{{ healthData.heartRate }}</text>
              <text class="unit">bpm</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section-container last">
        <view class="section-header">
          <text class="title">家庭动态</text>
        </view>
        <view
          class="family-card"
          v-for="member in familyMembers"
          :key="member.id"
        >
          <image :src="member.avatar" class="f-avatar" />
          <view class="f-info">
            <text class="f-name">{{ member.name }}</text>
            <text class="f-desc" :class="member.statusClass">{{
              member.status
            }}</text>
          </view>
          <text class="f-time">{{ member.time }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      refreshing: false,
      userInfo: { name: "陈涛", avatar: "", hasNew: true },
      hasNotification: true,

      // medicationList 现在匹配后端字段逻辑
      medicationList: [
        {
          id: 1,
          medicineName: "阿司匹林",
          timePoint: "10:00",
          dosage: "1片",
          status: 1,
        },
        {
          id: 2,
          medicineName: "降压药",
          timePoint: "14:00",
          dosage: "1片",
          status: 0,
        },
        {
          id: 3,
          medicineName: "维C",
          timePoint: "08:00",
          dosage: "2粒",
          status: 2,
        }, // 漏服演示
      ],

      functions: [
        {
          id: 1,
          name: "拍照识药",
          icon: "/static/Home/camera.svg",
          color: "#a8F2FF",
        },
        {
          id: 2,
          name: "就医清单",
          icon: "/static/Home/medical-list.svg",
          color: "#3B7ACC",
        },
        {
          id: 3,
          name: "家庭管理",
          icon: "/static/Home/family.svg",
          color: "#d8F9F2",
        },
        {
          id: 4,
          name: "AI 咨询",
          icon: "/static/Home/ai-assistant.svg",
          color: "#43E8FF",
        },
      ],

      healthData: { steps: 6234, heartRate: 72 },

      familyMembers: [
        {
          id: 1,
          name: "妈妈",
          avatar: "/static/avatars/avatar2.svg",
          status: "今日按时服药",
          statusClass: "s-green",
          time: "10分钟前",
        },
        {
          id: 2,
          name: "爸爸",
          avatar: "/static/avatars/avatar3.svg",
          status: "血压偏高需关注",
          statusClass: "s-orange",
          time: "30分钟前",
        },
      ],
    };
  },
  computed: {
    currentYear() {
      return new Date().getFullYear();
    },
    currentMonth() {
      return new Date().getMonth() + 1;
    },
    currentDay() {
      return new Date().getDate();
    },
    currentWeek() {
      return (
        "星期" + ["日", "一", "二", "三", "四", "五", "六"][new Date().getDay()]
      );
    },
  },
  methods: {
    // 状态样式映射 (规则 3.2 自动漏服检测体现)
    getStatusClass(status) {
      const map = {
        0: "status-pending", // 待服
        1: "status-done", // 已服
        2: "status-missed", // 漏服 (新增)
      };
      return map[status] || "";
    },

    async handleTakeMedicine(item) {
      // 调用修改任务状态接口
      uni.showLoading({ title: "确认中..." });
      setTimeout(() => {
        item.status = 1;
        uni.hideLoading();
        uni.showToast({ title: "已记录服用" });
      }, 500);
    },

    onRefresh() {
      this.refreshing = true;
      // 模拟重新获取今日任务 (规则 3.1)
      setTimeout(() => {
        this.refreshing = false;
        uni.showToast({ title: "已同步最新数据", icon: "none" });
      }, 800);
    },

    // 路由跳转方法 (保持你的原始逻辑)
    handleFunction(funcId) {
      const routes = {
        1: "/pages/scan/DrugScan",

        2: "/pages/medical/Prepare",

        3: "/pages/family/manage",

        4: "/pages/ai/assistant",
      };

      if (routes[funcId]) {
        uni.navigateTo({
          url: routes[funcId],
        });
      }
    },
    toProfile() {
      uni.navigateTo({
        url: "/pages/profile/profile",
      });
    },

    toNotification() {
      uni.navigateTo({
        url: "/pages/notification/notification",
      });
    },

    toMedicationList() {
      uni.navigateTo({
        url: "/pages/medication/list",
      });
    },

    toHealthDetail() {
      uni.navigateTo({
        url: "/pages/health/detail",
      });
    },
  },
};
</script>

<style lang="scss" scoped>
/* 变量定义 */
$primary-color: #5c62ff;
$bg-color: #f8f9fd;
$text-main: #1d1d2b;
$text-grey: #9fa0ab;
$status-green: #00c897;
$status-orange: #ff9f43;
$status-red: #ff5c5c;

.page-container {
  min-height: 100vh;
  background-color: $bg-color;
}

.status-bar {
  height: var(--status-bar-height);
}

/* 头部 Header */
.header-section {
  padding: 30rpx 40rpx 50rpx;
  background: linear-gradient(135deg, #ffffff 0%, #f0f4ff 100%);
  border-bottom-left-radius: 60rpx;
  border-bottom-right-radius: 60rpx;

  .user-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 40rpx;

    .user-info {
      display: flex;
      align-items: center;
      .avatar-img {
        width: 88rpx;
        height: 88rpx;
        border-radius: 50%;
        border: 4rpx solid #fff;
        box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
      }
      .welcome-text {
        margin-left: 20rpx;
        display: flex;
        flex-direction: column;
        .greeting {
          font-size: 24rpx;
          color: $text-grey;
        }
        .user-name {
          font-size: 32rpx;
          font-weight: bold;
          color: $text-main;
        }
      }
    }

    .icon-btn {
      position: relative;
      width: 80rpx;
      height: 80rpx;
      background: #fff;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.02);
      .btn-icon {
        width: 44rpx;
        height: 44rpx;
      }
      .dot {
        position: absolute;
        top: 20rpx;
        right: 20rpx;
        width: 14rpx;
        height: 14rpx;
        background: $status-red;
        border-radius: 50%;
        border: 4rpx solid #fff;
      }
    }
  }

  /* 日期卡片 */
  .date-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .date-left {
      display: flex;
      align-items: center;
      .big-day {
        font-size: 80rpx;
        font-weight: 800;
        color: $primary-color;
        line-height: 1;
      }
      .month-year {
        margin-left: 20rpx;
        display: flex;
        flex-direction: column;
        font-size: 26rpx;
        color: $text-main;
        font-weight: 500;
      }
    }
    .week-tag {
      background: rgba($primary-color, 0.1);
      color: $primary-color;
      padding: 10rpx 30rpx;
      border-radius: 30rpx;
      font-size: 24rpx;
      font-weight: bold;
    }
  }
}

/* 滚动区域 */
.main-content {
  height: calc(100vh - 350rpx);
  padding: 0 40rpx;
  box-sizing: border-box;
}

.section-container {
  margin-top: 50rpx;
  &.last {
    margin-bottom: 60rpx;
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    .title {
      font-size: 34rpx;
      font-weight: bold;
      color: $text-main;
    }
    .more-link {
      display: flex;
      align-items: center;
      font-size: 24rpx;
      color: $text-grey;
      .arrow-icon {
        width: 24rpx;
        height: 24rpx;
        margin-left: 4rpx;
      }
    }
  }
}

/* 任务卡片样式 (对应规则体现) */
.task-card {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 30rpx;
  border-radius: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);

  .task-time {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 100rpx;
    .time {
      font-size: 32rpx;
      font-weight: bold;
      color: $text-main;
    }
    .status-indicator {
      width: 12rpx;
      height: 12rpx;
      border-radius: 50%;
      background: #ddd;
      margin-top: 10rpx;
    }
  }

  .task-info {
    flex: 1;
    margin-left: 30rpx;
    .medicine-name {
      font-size: 30rpx;
      font-weight: bold;
      color: $text-main;
      display: block;
    }
    .dosage {
      font-size: 24rpx;
      color: $text-grey;
      margin-top: 4rpx;
    }
  }

  /* 不同状态的颜色表现 */
  &.status-pending .status-indicator {
    background: $primary-color;
  }
  &.status-done {
    opacity: 0.7;
    .status-indicator {
      background: $status-green;
    }
  }
  &.status-missed {
    background: #fff5f5;
    .status-indicator {
      background: $status-red;
    }
    .time {
      color: $status-red;
    }
  }

  .btn-confirm {
    background: $primary-color;
    color: #fff;
    font-size: 24rpx;
    padding: 10rpx 30rpx;
    border-radius: 16rpx;
    border: none;
    line-height: 1.5;
  }

  .status-label {
    font-size: 24rpx;
    font-weight: bold;
    &.done {
      color: $status-green;
    }
    &.missed {
      color: $status-red;
    }
    .icon {
      margin-right: 6rpx;
    }
  }
}

/* 快捷功能网格 */
.function-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  .func-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    .func-icon-bg {
      width: 100rpx;
      height: 100rpx;
      border-radius: 30rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12rpx;
      .func-icon {
        width: 50rpx;
        height: 50rpx;
      }
    }
    .func-name {
      font-size: 22rpx;
      color: $text-main;
      font-weight: 500;
    }
  }
}

/* 健康指标 */
.health-row {
  display: flex;
  gap: 24rpx;
  .health-card {
    flex: 1;
    background: #fff;
    padding: 24rpx;
    border-radius: 24rpx;
    .label {
      font-size: 22rpx;
      color: $text-grey;
    }
    .val-box {
      margin-top: 10rpx;
      .value {
        font-size: 40rpx;
        font-weight: 800;
        color: $text-main;
      }
      .unit {
        font-size: 20rpx;
        color: $text-grey;
        margin-left: 6rpx;
      }
    }
  }
}

/* 家庭动态 */
.family-card {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 24rpx;
  border-radius: 24rpx;
  margin-bottom: 20rpx;
  .f-avatar {
    width: 70rpx;
    height: 70rpx;
    border-radius: 50%;
  }
  .f-info {
    flex: 1;
    margin-left: 20rpx;
    .f-name {
      font-size: 28rpx;
      font-weight: bold;
      color: $text-main;
      display: block;
    }
    .f-desc {
      font-size: 22rpx;
    }
    .s-green {
      color: $status-green;
    }
    .s-orange {
      color: $status-orange;
    }
  }
  .f-time {
    font-size: 20rpx;
    color: $text-grey;
  }
}

.empty-state {
  text-align: center;
  padding: 60rpx;
  color: #ccc;
  font-size: 26rpx;
}
</style>
