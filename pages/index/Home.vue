<!-- pages/index/index.vue -->
<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header">
      <view class="user-info">
        <view class="avatar" @click="toProfile">
          <image 
            class="avatar-img" 
            :src="userInfo.avatar || '../../static/avatars/avatar1.svg'" 
            mode="aspectFill"
          />
          <view class="avatar-badge" v-if="userInfo.hasNew"></view>
        </view>
        <view class="user-name">{{ userInfo.name }}</view>
      </view>
      
      <view class="date-info">
        <text class="date">{{ currentDate }}</text>
        <text class="week">{{ currentWeek }}</text>
      </view>
      
      <view class="notification" @click="toNotification">
        <image 
          class="notification-icon" 
          src="../../static/Home/bell.svg" 
          mode="aspectFit"
        />
        <view class="notification-badge" v-if="hasNotification"></view>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view 
      class="main-content" 
      scroll-y 
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 服药提醒卡片 -->
      <view class="card medication-reminder">
        <view class="card-header">
          <image class="header-icon" src="../../static/Home/clock.svg" mode="aspectFit" />
          <text class="header-title">即将服药提醒</text>
          <view class="header-action" @click="toMedicationList">
            <text class="action-text">更多</text>
            <image class="action-icon" src="../../static/Home/right-arrow.svg" mode="aspectFit" />
          </view>
        </view>
        
        <view class="medication-item" v-for="item in medicationList" :key="item.id">
          <view class="medication-info">
            <view class="medication-time">{{ item.time }}</view>
            <view class="medication-name">{{ item.name }}</view>
            <view class="medication-detail">{{ item.dosage }}</view>
          </view>
          <button 
            class="action-btn" 
            :class="{ 'taken': item.taken }"
            @click="toggleMedication(item)"
          >
            {{ item.taken ? '已服用' : '服用' }}
          </button>
        </view>
      </view>

      <!-- 快捷功能卡片 -->
      <view class="card quick-functions">
        <view class="card-header">
          <image class="header-icon" src="../../static/Home/camera.svg" mode="aspectFit" />
          <text class="header-title">快捷功能</text>
        </view>
        
        <view class="function-grid">
          <view 
            class="function-item" 
            v-for="func in functions" 
            :key="func.id"
            @click="handleFunction(func.id)"
          >
            <view class="function-icon-container">
              <image class="function-icon" :src="func.icon" mode="aspectFit" />
            </view>
            <text class="function-name">{{ func.name }}</text>
          </view>
        </view>
      </view>

      <!-- 今日健康卡片 -->
      <view class="card health-data">
        <view class="card-header">
          <image class="header-icon" src="../../static/Home/heart.svg" mode="aspectFit" />
          <text class="header-title">今日健康</text>
          <view class="header-action" @click="toHealthDetail">
            <text class="action-text">详情</text>
            <image class="action-icon" src="../../static/Home/right-arrow.svg" mode="aspectFit" />
          </view>
        </view>
        
        <view class="health-grid">
          <view class="health-item">
            <view class="health-value">{{ healthData.steps.toLocaleString() }}</view>
            <text class="health-label">步数</text>
            <image class="trend-icon" src="../../static/Home/trend-up.svg" mode="aspectFit" />
          </view>
          <view class="health-item">
            <view class="health-value">{{ healthData.heartRate }}</view>
            <text class="health-label">心率</text>
            <text class="health-unit">bpm</text>
          </view>
          <view class="health-item">
            <view class="health-value">{{ healthData.bloodPressure }}</view>
            <text class="health-label">血压</text>
          </view>
          <view class="health-item">
            <view class="health-value">{{ healthData.sleep }}</view>
            <text class="health-label">睡眠</text>
            <text class="health-unit">h</text>
          </view>
        </view>
      </view>

      <!-- 家庭动态卡片 -->
      <view class="card family-dynamic">
        <view class="card-header">
          <image class="header-icon" src="../../static/Home/family.svg" mode="aspectFit" />
          <text class="header-title">家庭动态</text>
          <view class="header-action" @click="toFamilyManagement">
            <text class="action-text">管理</text>
            <image class="action-icon" src="../../static/Home/right-arrow.svg" mode="aspectFit" />
          </view>
        </view>
        
        <view class="family-list">
          <view class="family-item" v-for="member in familyMembers" :key="member.id">
            <view class="member-avatar">
              <image class="member-avatar-img" :src="member.avatar" mode="aspectFill" />
            </view>
            <view class="member-info">
              <view class="member-name">{{ member.name }}</view>
              <view class="member-status">
                <text class="status-text" :class="member.statusClass">{{ member.status }}</text>
                <image 
                  class="status-icon" 
                  :src="member.statusIcon" 
                  mode="aspectFit" 
                />
              </view>
            </view>
            <text class="update-time">{{ member.time }}</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>

export default {
  name:"HomePage",
  data() {
    return {
      refreshing: false,
      userInfo: {
        name: '陈涛',
        avatar: '../../static/avatars/avatar1.svg',
        hasNew: true
      },
      hasNotification: true,
      medicationList: [
        {
          id: 1,
          name: '阿司匹林肠溶片',
          time: '10:00',
          dosage: '每日1次，每次1片',
          taken: true
        },
        {
          id: 2,
          name: '降压药',
          time: '14:00',
          dosage: '每日2次，每次1片',
          taken: false
        }
      ],
      functions: [
        { id: 1, name: '拍照识药', icon: '../../static/Home/camera-drug.svg' },
        { id: 2, name: '就医准备单', icon: '../../static/Home/medical-list.svg' },
        { id: 3, name: '家庭管理', icon: '../../static/Home/family-manage.svg' },
        { id: 4, name: '问医小助手', icon: '../../static/Home/ai-assistant.svg' }
      ],
      healthData: {
        steps: 6234,
        heartRate: 72,
        bloodPressure: '120/80',
        sleep: '7.5'
      },
      familyMembers: [
        {
          id: 1,
          name: '妈妈',
          avatar: '../../static/avatars/avatar2.svg',
          status: '今日按时服药',
          statusClass: 'status-success',
          statusIcon: '../../static/Home/check.svg',
          time: '10分钟前'
        },
        {
          id: 2,
          name: '爸爸',
          avatar: '../../static/avatars/avatar3.svg',
          status: '血压偏高需关注',
          statusClass: 'status-warning',
          statusIcon: '../../static/Home/warning.svg',
          time: '30分钟前'
        }
      ]
    }
  },
  computed: {
    currentDate() {
      const date = new Date()
      return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
    },
    currentWeek() {
      const weeks = ['日', '一', '二', '三', '四', '五', '六']
      const date = new Date()
      return `周${weeks[date.getDay()]}`
    }
  },
  methods: {
    onRefresh() {
      this.refreshing = true
      // 模拟数据刷新
      setTimeout(() => {
        this.refreshing = false
        uni.showToast({
          title: '刷新成功',
          icon: 'success'
        })
      }, 1000)
    },
    toProfile() {
      uni.navigateTo({
        url: '/pages/profile/profile'
      })
    },
    toNotification() {
      uni.navigateTo({
        url: '/pages/notification/notification'
      })
    },
    toMedicationList() {
      uni.navigateTo({
        url: '/pages/medication/list'
      })
    },
    toHealthDetail() {
      uni.navigateTo({
        url: '/pages/health/detail'
      })
    },
    toFamilyManagement() {
      uni.navigateTo({
        url: '/pages/family/manage'
      })
    },
    toggleMedication(item) {
      item.taken = !item.taken
      uni.showToast({
        title: item.taken ? '已标记为已服用' : '已取消服用',
        icon: 'success'
      })
    },
    handleFunction(funcId) {
      const routes = {
        1: '/pages/scan/DrugScan',
        2: '/pages/medical/Prepare',
        3: '/pages/family/manage',
        4: '/pages/ai/assistant'
      }
      
      if (routes[funcId]) {
        uni.navigateTo({
          url: routes[funcId]
        })
      }
    }
  }
}
</script>

<style scoped lang="scss">
.page-container {
    width: 100%;
  height: 100vh;
  background: linear-gradient(180deg, #f8faff 0%, #ffffff 100%);
  padding-bottom: 120rpx;
}

/* 头部样式 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40rpx 32rpx 20rpx;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 0 0 32rpx 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.15);
}

.user-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.avatar {
  position: relative;
  width: 80rpx;
  height: 80rpx;
  margin-right: 20rpx;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  background: #fff;
}

.avatar-badge {
  position: absolute;
  top: 0;
  right: 0;
  width: 20rpx;
  height: 20rpx;
  background: #ff6b6b;
  border: 3rpx solid #fff;
  border-radius: 50%;
}

.user-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 1rpx;
}

.date-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
}

.date {
  font-size: 28rpx;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.95);
}

.week {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4rpx;
}

.notification {
  position: relative;
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.notification-icon {
  width: 44rpx;
  height: 44rpx;
}

.notification-badge {
  position: absolute;
  top: -4rpx;
  right: -4rpx;
  width: 16rpx;
  height: 16rpx;
  background: #ff6b6b;
  border-radius: 50%;
  border: 2rpx solid #2d6bff;
}

/* 主要内容区域 */
/* 直接在 .main-content 上添加这些样式 */
.main-content {
  height: calc(100vh - 240rpx);
  padding: 32rpx 32rpx 0 32rpx;
  box-sizing: border-box;
  
  /* 关键：禁止横向滚动 */
  overflow-x: hidden !important;
  overflow-y: auto;
  
  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
}

/* 针对Webkit浏览器隐藏滚动条 */
.main-content::-webkit-scrollbar {
  display: none;
  width: 0;
}

/* 卡片通用样式 */
.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
}

.header-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 16rpx;
}

.header-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #2d3b4e;
  flex: 1;
}

.header-action {
  display: flex;
  align-items: center;
  padding: 8rpx 16rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 20rpx;
  transition: all 0.3s ease;
}

.header-action:active {
  background: rgba(77, 142, 255, 0.2);
}

.action-text {
  font-size: 26rpx;
  color: #4d8eff;
  margin-right: 8rpx;
}

.action-icon {
  width: 20rpx;
  height: 20rpx;
}

/* 服药提醒卡片 */
.medication-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  background: linear-gradient(135deg, rgba(77, 142, 255, 0.05) 0%, rgba(45, 107, 255, 0.03) 100%);
  border-radius: 20rpx;
  margin-bottom: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.15);
  
  &:last-child {
    margin-bottom: 0;
  }
}

.medication-info {
  flex: 1;
}

.medication-time {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b6b;
  margin-bottom: 8rpx;
}

.medication-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
  margin-bottom: 4rpx;
}

.medication-detail {
  font-size: 26rpx;
  color: #87909c;
}

.action-btn {
  padding: 16rpx 32rpx;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 20rpx;
  border: none;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 600;
  transition: all 0.3s ease;
  
  &.taken {
    background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
  }
}

.action-btn:active {
  transform: scale(0.95);
}

/* 快捷功能卡片 */
.function-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32rpx;
}

.function-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx 24rpx;
  background: linear-gradient(135deg, rgba(77, 142, 255, 0.08) 0%, rgba(45, 107, 255, 0.04) 100%);
  border-radius: 24rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.15);
  transition: all 0.3s ease;
}

.function-item:active {
  transform: translateY(-4rpx);
  background: linear-gradient(135deg, rgba(77, 142, 255, 0.12) 0%, rgba(45, 107, 255, 0.08) 100%);
  box-shadow: 0 8rpx 32rpx rgba(45, 107, 255, 0.15);
}

.function-icon-container {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 20rpx;
  margin-bottom: 20rpx;
}

.function-icon {
  width: 40rpx;
  height: 40rpx;
}

.function-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #2d3b4e;
}

/* 今日健康卡片 */
.health-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32rpx;
}

.health-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx 24rpx;
  background: linear-gradient(135deg, rgba(77, 142, 255, 0.05) 0%, rgba(45, 107, 255, 0.02) 100%);
  border-radius: 20rpx;
  position: relative;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.health-value {
  font-size: 44rpx;
  font-weight: 700;
  color: #2d6bff;
  margin-bottom: 8rpx;
}

.health-label {
  font-size: 26rpx;
  color: #87909c;
}

.health-unit {
  font-size: 24rpx;
  color: #87909c;
  margin-left: 4rpx;
}

.trend-icon {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  width: 48rpx;
  height: 48rpx;
}

/* 家庭动态卡片 */
.family-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.family-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: linear-gradient(135deg, rgba(77, 142, 255, 0.05) 0%, rgba(45, 107, 255, 0.02) 100%);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.member-avatar {
  width: 80rpx;
  height: 80rpx;
  margin-right: 20rpx;
}

.member-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 4rpx solid rgba(77, 142, 255, 0.2);
}

.member-info {
  flex: 1;
}

.member-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
  margin-bottom: 8rpx;
}

.member-status {
  display: flex;
  align-items: center;
}

.status-text {
  font-size: 26rpx;
  margin-right: 12rpx;
  
  &.status-success {
    color: #10b981;
  }
  
  &.status-warning {
    color: #f59e0b;
  }
}

.status-icon {
  width: 36rpx;
  height: 36rpx;
}

.update-time {
  font-size: 24rpx;
  color: #87909c;
}
</style>