<!-- pages/profile/index.vue -->
<template>
  <view class="page-container">
    <!-- 顶部用户信息卡片 -->
    <view class="user-card">
      <view class="user-header" @click="editProfile">
        <view class="avatar-section">
          <view class="avatar-container">
            <image 
              class="avatar" 
              :src="userInfo.avatar || '../../static/avatars/avatar1.svg'" 
              mode="aspectFill"
            />
            <view class="avatar-badge" v-if="userInfo.hasUpdate"></view>
          </view>
          <view class="user-basic">
            <text class="user-name">{{ userInfo.name }}</text>
            <view class="user-tags">
              <view class="tag-item gender">
                <image class="tag-icon" src="../../static/Mine/gender.svg" mode="aspectFit" />
                <text class="tag-text">{{ userInfo.gender }}</text>
              </view>
              <view class="tag-item age">
                <image class="tag-icon" src="../../static/Mine/age.png" mode="aspectFit" />
                <text class="tag-text">{{ userInfo.age }}岁</text>
              </view>
              <view class="tag-item health">
                <image class="tag-icon" src="../../static/Mine/health-file.png" mode="aspectFit" />
                <text class="tag-text">{{ userInfo.healthFile }}</text>
              </view>
            </view>
          </view>
        </view>
        <image class="right-arrow" src="../../static/Mine/right-arrow.svg" mode="aspectFit" />
      </view>
      
      <!-- 健康概览 -->
      <view class="health-overview">
        <view 
          class="health-item"
          v-for="item in healthOverview"
          :key="item.id"
          @click="toHealthDetail(item.type)"
        >
          <view class="health-value">
            <text class="value-number">{{ item.value }}</text>
            <text class="value-unit">{{ item.unit }}</text>
          </view>
          <text class="health-label">{{ item.label }}</text>
          <image 
            class="trend-icon" 
            :src="item.trendIcon" 
            mode="aspectFit"
            v-if="item.trend"
          />
        </view>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view 
      class="main-content"
      scroll-y
      :show-scrollbar="false"
      :scroll-x="false"
    >
      <!-- 常用功能 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">常用功能</text>
        </view>
        <view class="menu-list">
          <view 
            class="menu-item"
            v-for="item in commonFunctions"
            :key="item.id"
            @click="handleFunction(item.id)"
          >
            <view class="menu-icon-container">
              <image class="menu-icon" :src="item.icon" mode="aspectFit" />
            </view>
            <view class="menu-content">
              <text class="menu-title">{{ item.title }}</text>
              <text class="menu-desc" v-if="item.desc">{{ item.desc }}</text>
            </view>
            <view class="menu-right">
              <text class="menu-badge" v-if="item.badge">{{ item.badge }}</text>
              <image class="menu-arrow" src="../../static/Mine/right-arrow.svg" mode="aspectFit" />
            </view>
          </view>
        </view>
      </view>

      <!-- 数据与隐私 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">数据与隐私</text>
        </view>
        <view class="menu-list">
          <view 
            class="menu-item"
            v-for="item in privacyFunctions"
            :key="item.id"
            @click="handleFunction(item.id)"
          >
            <view class="menu-icon-container" :class="item.iconClass">
              <image class="menu-icon" :src="item.icon" mode="aspectFit" />
            </view>
            <view class="menu-content">
              <text class="menu-title">{{ item.title }}</text>
              <text class="menu-desc" v-if="item.desc">{{ item.desc }}</text>
            </view>
            <view class="menu-right">
              <view class="privacy-status" v-if="item.status">
                <text class="status-text">{{ item.status }}</text>
              </view>
              <image class="menu-arrow" src="../../static/Mine/right-arrow.svg" mode="aspectFit" />
            </view>
          </view>
        </view>
      </view>

      <!-- 其他 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">其他</text>
        </view>
        <view class="menu-list">
          <view 
            class="menu-item"
            v-for="item in otherFunctions"
            :key="item.id"
            @click="handleFunction(item.id)"
          >
            <view class="menu-icon-container" :class="item.iconClass">
              <image class="menu-icon" :src="item.icon" mode="aspectFit" />
            </view>
            <view class="menu-content">
              <text class="menu-title">{{ item.title }}</text>
              <text class="menu-desc" v-if="item.desc">{{ item.desc }}</text>
            </view>
            <view class="menu-right">
              <text class="version-text" v-if="item.id === 'about'">{{ appVersion }}</text>
              <image class="menu-arrow" src="../../static/Mine/right-arrow.svg" mode="aspectFit" />
            </view>
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
  name:"MinePage",
  data() {
    return {
      userInfo: {
        name: '陈涛',
        avatar: '../../static/avatars/avatar1.svg',
        gender: '男',
        age: 21,
        healthFile: '健康档案',
        hasUpdate: true
      },
      healthOverview: [
        {
          id: 1,
          type: 'bloodPressure',
          value: '120/80',
          unit: 'mmHg',
          label: '血压',
          trend: 'stable',
          trendIcon: '../../static/Mine/trend-stable.svg'
        },
        {
          id: 2,
          type: 'heartRate',
          value: '72',
          unit: 'bpm',
          label: '心率',
          trend: 'stable',
          trendIcon: '../../static/Mine/trend-stable.svg'
        },
        {
          id: 3,
          type: 'bloodSugar',
          value: '5.2',
          unit: 'mmol/L',
          label: '血糖',
          trend: 'down',
          trendIcon: '../../static/Mine/trend-down.svg'
        }
      ],
      commonFunctions: [
        {
          id: 'reminder',
          title: '提醒设置',
          desc: '服药、复查提醒',
          icon: '../../static/Mine/bell-setting.svg',
          badge: '3'
        },
        {
          id: 'family',
          title: '家庭成员管理',
          desc: '管理家人健康数据',
          icon: '../../static/Mine/family.svg'
        },
        {
          id: 'reports',
          title: '历史健康报告',
          desc: '查看历史健康报告',
          icon: '../../static/Mine/report.svg'
        },
        {
          id: 'medicalRecords',
          title: '就医记录',
          desc: '门诊、住院记录',
          icon: '../../static/Mine/medical-record.svg',
          badge: '5'
        }
      ],
      privacyFunctions: [
        {
          id: 'privacy',
          title: '隐私保护',
          desc: '联邦学习技术保护',
          icon: '../../static/Mine/lock.svg',
          iconClass: 'icon-privacy',
          status: '已启用'
        },
        {
          id: 'export',
          title: '数据导出',
          desc: '导出健康数据',
          icon: '../../static/Mine/export.svg',
          iconClass: 'icon-export'
        },
        {
          id: 'clear',
          title: '清除本地数据',
          desc: '清空缓存和本地数据',
          icon: '../../static/Mine/trash.svg',
          iconClass: 'icon-clear'
        }
      ],
      otherFunctions: [
        {
          id: 'settings',
          title: '通用设置',
          desc: '应用设置与偏好',
          icon: '../../static/Mine/settings.svg',
          iconClass: 'icon-settings'
        },
        {
          id: 'help',
          title: '帮助与反馈',
          desc: '使用帮助与问题反馈',
          icon: '../../static/Mine/help.svg',
          iconClass: 'icon-help'
        },
        {
          id: 'about',
          title: '关于我们',
          desc: '版本信息与介绍',
          icon: '../../static/Mine/info.svg',
          iconClass: 'icon-about'
        }
      ],
      appVersion: 'v1.2.0'
    }
  },
  onShow() {
    this.loadUserInfo()
  },
  methods: {
    loadUserInfo() {
      // 加载用户信息
      // 可以从本地存储或API获取
    },
    editProfile() {
      uni.navigateTo({
        url: '/pages/profile/edit'
      })
    },
    toHealthDetail(type) {
      const routes = {
        bloodPressure: '/pages/health/detail?type=bloodPressure',
        heartRate: '/pages/health/detail?type=heartRate',
        bloodSugar: '/pages/health/detail?type=bloodSugar'
      }
      
      if (routes[type]) {
        uni.navigateTo({
          url: routes[type]
        })
      }
    },
    handleFunction(id) {
      const routes = {
        reminder: '/pages/reminder/setting',
        family: '/pages/family/manage',
        reports: '/pages/health/reports',
        medicalRecords: '/pages/medical/records',
        privacy: '/pages/privacy/setting',
        export: '/pages/data/export',
        clear: '/pages/data/clear',
        settings: '/pages/settings/index',
        help: '/pages/help/index',
        about: '/pages/about/index'
      }
      
      if (routes[id]) {
        uni.navigateTo({
          url: routes[id]
        })
        return
      }
      
      // 处理特殊功能
      switch (id) {
        case 'clear':
          this.clearLocalData()
          break
      }
    },
    clearLocalData() {
      uni.showModal({
        title: '清除本地数据',
        content: '确定要清除所有本地缓存数据吗？此操作不可恢复。',
        confirmColor: '#FF6B6B',
        success: (res) => {
          if (res.confirm) {
            uni.showLoading({
              title: '清除中...'
            })
            
            // 模拟清除操作
            setTimeout(() => {
              uni.hideLoading()
              uni.showToast({
                title: '清除完成',
                icon: 'success'
              })
              
              // 重新加载数据
              this.loadUserInfo()
            }, 1500)
          }
        }
      })
    },
    logout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.showLoading({
              title: '退出中...'
            })
            
            // 模拟退出操作
            setTimeout(() => {
              uni.hideLoading()
              
              // 清除登录状态
              uni.removeStorageSync('token')
              uni.removeStorageSync('userInfo')
              
              // 跳转到登录页
              uni.reLaunch({
                url: '/pages/login/index'
              })
            }, 1000)
          }
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8faff 0%, #f0f5ff 100%);
  padding-bottom: 120rpx;
}

/* 用户卡片 */
.user-card {
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 0 0 32rpx 32rpx;
  padding: 60rpx 32rpx 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.15);
  margin-bottom: 32rpx;
}

.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40rpx;
  
  &:active {
    opacity: 0.9;
  }
}

.avatar-section {
  display: flex;
  align-items: center;
  flex: 1;
}

.avatar-container {
  position: relative;
  width: 120rpx;
  height: 120rpx;
  margin-right: 24rpx;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 6rpx solid rgba(255, 255, 255, 0.3);
  background: #ffffff;
}

.avatar-badge {
  position: absolute;
  top: 0;
  right: 0;
  width: 24rpx;
  height: 24rpx;
  background: #ff6b6b;
  border: 4rpx solid #2d6bff;
  border-radius: 50%;
}

.user-basic {
  flex: 1;
}

.user-name {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 16rpx;
}

.user-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.tag-item {
  display: flex;
  align-items: center;
  padding: 8rpx 16rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
  backdrop-filter: blur(10rpx);
}

.tag-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 6rpx;
}

.tag-text {
  font-size: 24rpx;
  color: #ffffff;
  font-weight: 500;
}

.right-arrow {
  width: 24rpx;
  height: 24rpx;
}

/* 健康概览 */
.health-overview {
  display: flex;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 24rpx;
  padding: 32rpx 24rpx;
  backdrop-filter: blur(10rpx);
  border: 2rpx solid rgba(255, 255, 255, 0.2);
}

.health-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 12rpx;
  position: relative;
  
  &:active {
    opacity: 0.9;
  }
}

.health-item + .health-item {
  border-left: 2rpx solid rgba(255, 255, 255, 0.2);
}

.health-value {
  display: flex;
  align-items: baseline;
  margin-bottom: 8rpx;
}

.value-number {
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
  line-height: 1;
}

.value-unit {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  margin-left: 4rpx;
}

.health-label {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.trend-icon {
  position: absolute;
  top: -8rpx;
  right: 0;
  width: 24rpx;
  height: 24rpx;
}

/* 主要内容区域 */
.main-content {
  height: calc(100vh - 580rpx);
  padding: 0 32rpx;
  box-sizing: border-box;
  overflow-x: hidden !important;
}

.main-content::-webkit-scrollbar {
  display: none;
}

/* 分区样式 */
.section {
  margin-bottom: 32rpx;
}

.section-header {
  margin-bottom: 24rpx;
  padding: 0 8rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2d3b4e;
}

/* 菜单列表 */
.menu-list {
  background: #ffffff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 32rpx;
  position: relative;
  transition: all 0.3s ease;
  
  &:active {
    background: rgba(77, 142, 255, 0.05);
  }
  
  &::after {
    content: '';
    position: absolute;
    left: 120rpx;
    right: 32rpx;
    bottom: 0;
    height: 2rpx;
    background: linear-gradient(90deg, transparent 0%, rgba(77, 142, 255, 0.1) 50%, transparent 100%);
  }
  
  &:last-child::after {
    display: none;
  }
}

.menu-icon-container {
  width: 64rpx;
  height: 64rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  flex-shrink: 0;
  
  &.icon-privacy {
    background: linear-gradient(135deg, rgba(77, 142, 255, 0.15) 0%, rgba(45, 107, 255, 0.1) 100%);
  }
  
  &.icon-export {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(16, 185, 129, 0.1) 100%);
  }
  
  &.icon-clear {
    background: linear-gradient(135deg, rgba(255, 107, 107, 0.15) 0%, rgba(255, 107, 107, 0.1) 100%);
  }
  
  &.icon-settings {
    background: linear-gradient(135deg, rgba(180, 191, 211, 0.15) 0%, rgba(180, 191, 211, 0.1) 100%);
  }
  
  &.icon-help {
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.15) 0%, rgba(245, 158, 11, 0.1) 100%);
  }
  
  &.icon-about {
    background: linear-gradient(135deg, rgba(139, 92, 246, 0.15) 0%, rgba(139, 92, 246, 0.1) 100%);
  }
}

.menu-icon {
  width: 32rpx;
  height: 32rpx;
}

.menu-content {
  flex: 1;
  min-width: 0;
}

.menu-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
  margin-bottom: 8rpx;
}

.menu-desc {
  display: block;
  font-size: 24rpx;
  color: #87909c;
}

.menu-right {
  display: flex;
  align-items: center;
  margin-left: 16rpx;
}

.menu-badge {
  padding: 6rpx 16rpx;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff5252 100%);
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 600;
  border-radius: 16rpx;
  min-width: 32rpx;
  text-align: center;
  margin-right: 16rpx;
}

.privacy-status {
  padding: 6rpx 16rpx;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 16rpx;
  margin-right: 16rpx;
}

.status-text {
  font-size: 22rpx;
  color: #10b981;
  font-weight: 500;
}

.version-text {
  font-size: 24rpx;
  color: #87909c;
  margin-right: 16rpx;
}

.menu-arrow {
  width: 24rpx;
  height: 24rpx;
}

/* 退出登录 */
.logout-section {
  margin-top: 48rpx;
  margin-bottom: 32rpx;
}

.logout-btn {
  width: 54%;
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