<!-- pages/reminder/setting.vue -->
<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header">
      <view class="header-left">
        <view class="back-btn" @click="goBack">
          <image class="back-icon" src="../../static/Register/back.png" mode="aspectFit" />
        </view>
        <text class="page-title">服药提醒设置</text>
      </view>
      <button class="add-btn" @click="addReminder">
        <image class="add-icon" src="../../static/Health/plus-circle.svg" mode="aspectFit" />
      </button>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view 
      class="main-content"
      scroll-y
      :show-scrollbar="false"
    >
      <!-- 今天的提醒 -->
      <view class="section">
        <text class="section-title">今天的提醒</text>
        <view class="reminder-list">
          <view 
            class="reminder-card"
            v-for="reminder in todayReminders"
            :key="reminder.id"
          >
            <view class="reminder-header">
              <image class="clock-icon" src="../../static/Home/clock.svg" mode="aspectFit" />
              <text class="reminder-time">{{ reminder.time }}</text>
              <text class="reminder-name">{{ reminder.name }}</text>
            </view>
            
            <view class="reminder-options">
              <view class="option-item">
                <image class="option-icon" :src="reminder.noticeType.icon" mode="aspectFit" />
                <text class="option-text">{{ reminder.noticeType.name }}</text>
                <switch 
                  class="option-switch"
                  :checked="reminder.enabled"
                  @change="toggleReminder(reminder.id)"
                  color="#4d8eff"
                />
              </view>
              
              <view class="option-item">
                <image class="option-icon" src="../../static/Home/bell.svg" mode="aspectFit" />
                <text class="option-text">提前{{ reminder.advanceTime }}提醒</text>
              </view>
              
              <view class="option-item">
                <image class="option-icon" src="../../static/DrugScan/refresh.svg" mode="aspectFit" />
                <text class="option-text">{{ reminder.repeat }}</text>
              </view>
            </view>
            
            <button class="edit-btn" @click="editReminder(reminder.id)">
              <text class="edit-text">编辑</text>
            </button>
          </view>
        </view>
      </view>

      <!-- 提醒风格设置 -->
      <view class="section">
        <text class="section-title">提醒风格设置</text>
        <view class="style-settings">
          <view class="setting-item" @click="selectRingtone">
            <view class="setting-left">
              <image class="setting-icon" src="../../static/Reminder/music.svg" mode="aspectFit" />
              <text class="setting-label">铃声选择</text>
            </view>
            <view class="setting-right">
              <text class="setting-value">{{ settings.ringtone }}</text>
              <image class="arrow-icon" src="../../static/Home/right-arrow.svg" mode="aspectFit" />
            </view>
          </view>
          
          <view class="setting-item" @click="selectVibration">
            <view class="setting-left">
              <image class="setting-icon" src="../../static/Reminder/vibrate.svg" mode="aspectFit" />
              <text class="setting-label">震动强度</text>
            </view>
            <view class="setting-right">
              <text class="setting-value">{{ settings.vibration }}</text>
              <image class="arrow-icon" src="../../static/Home/right-arrow.svg" mode="aspectFit" />
            </view>
          </view>
          
          <view class="setting-item" @click="adjustVolume">
            <view class="setting-left">
              <image class="setting-icon" src="../../static/Reminder/volume.svg" mode="aspectFit" />
              <text class="setting-label">音量大小</text>
            </view>
            <view class="setting-right">
              <view class="volume-slider">
                <view 
                  class="volume-track"
                  @click="setVolume"
                >
                  <view 
                    class="volume-fill"
                    :style="{ width: settings.volume + '%' }"
                  ></view>
                </view>
                <view 
                  class="volume-thumb"
                  :style="{ left: settings.volume + '%' }"
                ></view>
              </view>
              <image class="arrow-icon" src="../../static/Home/right-arrow.svg" mode="aspectFit" />
            </view>
          </view>
          
          <view class="setting-item" @click="selectMessage">
            <view class="setting-left">
              <image class="setting-icon" src="../../static/Reminder/message.svg" mode="aspectFit" />
              <text class="setting-label">提醒文案</text>
            </view>
            <view class="setting-right">
              <text class="setting-value">{{ settings.messageType }}</text>
              <image class="arrow-icon" src="../../static/Home/right-arrow.svg" mode="aspectFit" />
            </view>
          </view>
          
          <view class="message-preview">
            <text class="preview-text">示例："{{ settings.messageExample }}"</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      todayReminders: [
        {
          id: 1,
          time: '10:00',
          name: '阿司匹林',
          enabled: true,
          noticeType: {
            icon: '../../static/Home/bell.svg',
            name: '铃声 + 震动'
          },
          advanceTime: '5分钟',
          repeat: '每天重复'
        },
        {
          id: 2,
          time: '19:00',
          name: '二甲双胍',
          enabled: true,
          noticeType: {
            icon: '../../static/Reminder/voice.svg',
            name: '语音播报'
          },
          advanceTime: '10分钟',
          repeat: '每天重复'
        }
      ],
      settings: {
        ringtone: '温馨提示音',
        vibration: '中等',
        volume: 80,
        messageType: '关怀版',
        messageExample: '该吃药啦，记得配温水'
      }
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    addReminder() {
      uni.navigateTo({
        url: '/pages/reminder/add'
      })
    },
    toggleReminder(id) {
      const reminder = this.todayReminders.find(r => r.id === id)
      if (reminder) {
        reminder.enabled = !reminder.enabled
        uni.showToast({
          title: reminder.enabled ? '已开启' : '已关闭',
          icon: 'success'
        })
      }
    },
    editReminder(id) {
      uni.navigateTo({
        url: `/pages/reminder/edit?id=${id}`
      })
    },
    selectRingtone() {
      uni.showActionSheet({
        itemList: ['温馨提示音', '柔和铃声', '清脆铃声', '默认铃声'],
        success: (res) => {
          const ringtones = ['温馨提示音', '柔和铃声', '清脆铃声', '默认铃声']
          this.settings.ringtone = ringtones[res.tapIndex]
        }
      })
    },
    selectVibration() {
      uni.showActionSheet({
        itemList: ['关闭', '轻柔', '中等', '强烈'],
        success: (res) => {
          const vibrations = ['关闭', '轻柔', '中等', '强烈']
          this.settings.vibration = vibrations[res.tapIndex]
        }
      })
    },
    adjustVolume() {
      uni.showModal({
        title: '调整音量',
        content: '请在系统设置中调整音量',
        showCancel: false,
        confirmText: '知道了'
      })
    },
    setVolume(e) {
      // 点击设置音量（简化版）
      console.log('点击设置音量')
    },
    selectMessage() {
      uni.showActionSheet({
        itemList: ['关怀版', '简洁版', '专业版', '自定义'],
        success: (res) => {
          const types = ['关怀版', '简洁版', '专业版', '自定义']
          const examples = [
            '该吃药啦，记得配温水',
            '服药时间到',
            '请按时服用药物',
            '点击编辑自定义文案'
          ]
          this.settings.messageType = types[res.tapIndex]
          this.settings.messageExample = examples[res.tapIndex]
        }
      })
    }
  }
}
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background: #f8faff;
}

/* 头部导航栏 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 32rpx 24rpx;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 0 0 32rpx 32rpx;
}

.header-left {
  display: flex;
  align-items: center;
}

.back-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  margin-right: 20rpx;
}

.back-icon {
  width: 32rpx;
  height: 32rpx;
}

.page-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;

  margin-left: auto;
  margin-right: 0;
  height: 64rpx;
  width: 64rpx;
  padding: 0;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 32rpx;
  border: none;
}

.add-icon {
  width: 48rpx;
  height: 48rpx;
}

.add-text {
  font-size: 26rpx;
  color: #ffffff;
  font-weight: 500;
}

/* 主要内容区域 */
.main-content {
  height: calc(100vh - 200rpx);
  width: 100%;
  padding: 32rpx;
  box-sizing: border-box; /* 关键属性 */
}

/* 分区 */
.section {
  margin-bottom: 40rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #2d3b4e;
  margin-bottom: 24rpx;
}

/* 提醒卡片 */
.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.reminder-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.08);
  border: 1rpx solid rgba(77, 142, 255, 0.1);
}

.reminder-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.clock-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
}

.reminder-time {
  font-size: 36rpx;
  font-weight: 700;
  color: #4d8eff;
  margin-right: 20rpx;
}

.reminder-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
}

/* 提醒选项 */
.reminder-options {
  margin-bottom: 24rpx;
}

.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
  padding-left: 44rpx;
}

.option-item:last-child {
  margin-bottom: 0;
}

.option-icon {
  width: 28rpx;
  height: 28rpx;
  margin-right: 16rpx;
}

.option-text {
  font-size: 26rpx;
  color: #555e6d;
  flex: 1;
}

.option-switch {
  transform: scale(0.8);
}

/* 编辑按钮 */
.edit-btn {
  flex: 1;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28rpx 0;
  border-radius: 48rpx;
  border: none;
  font-size: 28rpx;
  font-weight: 600;
}

/* 风格设置 */
.style-settings {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.08);
  border: 1rpx solid rgba(77, 142, 255, 0.1);
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid rgba(77, 142, 255, 0.1);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-left {
  display: flex;
  align-items: center;
}

.setting-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 20rpx;
}

.setting-label {
  font-size: 30rpx;
  color: #2d3b4e;
}

.setting-right {
  display: flex;
  align-items: center;
}

.setting-value {
  font-size: 28rpx;
  color: #87909c;
  margin-right: 16rpx;
}

.arrow-icon {
  width: 24rpx;
  height: 24rpx;
}

/* 音量滑块 */
.volume-slider {
  position: relative;
  width: 200rpx;
  height: 40rpx;
  margin-right: 16rpx;
}

.volume-track {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 8rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 4rpx;
  transform: translateY(-50%);
}

.volume-fill {
  height: 100%;
  background: linear-gradient(90deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 4rpx;
}

.volume-thumb {
  position: absolute;
  top: 50%;
  width: 32rpx;
  height: 32rpx;
  background: #ffffff;
  border: 4rpx solid #4d8eff;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 4rpx 12rpx rgba(45, 107, 255, 0.3);
}

/* 消息预览 */
.message-preview {
  margin-top: 32rpx;
  padding: 24rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  text-align: center;
}

.preview-text {
  font-size: 26rpx;
  color: #4d8eff;
  line-height: 1.4;
}
</style>