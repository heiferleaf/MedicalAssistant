<!-- pages/drug/scan.vue -->
<template>
  <view class="page-container">
    <!-- 顶部导航 -->
    <view class="header">
      <view class="header-left">
        <view class="back-btn" @click="goBack">
          <image class="back-icon" src="../../static/Register/back.png" mode="aspectFit" />
        </view>
        <text class="page-title">药物识别</text>
      </view>
      <view class="header-right">
        <button class="flash-btn" @click="toggleFlash">
          <image 
            class="flash-icon" 
            :src="flashOn ? '../../static/DrugScan/flash-on.svg' : '../../static/DrugScan/flash-off.svg'" 
            mode="aspectFit"
          />
        </button>
      </view>
    </view>

    <!-- 相机界面 -->
    <view class="camera-container" v-if="pageState === 'camera'">
      <camera 
        ref="camera"
        class="camera"
        device-position="back"
        flash="off"
        @error="cameraError"
      >
        <!-- 对焦框 -->
        <view class="focus-area" v-if="showFocus" :style="focusStyle"></view>
      </camera>
      
      <!-- 操作按钮 -->
      <view class="camera-controls">
        <view class="control-buttons">
          <button class="control-btn gallery-btn" @click="chooseImage">
            <image class="gallery-icon" src="../../static/DrugScan/gallery.svg" mode="aspectFit" />
            <text class="btn-text">相册</text>
          </button>
          
          <button class="control-btn capture-btn" @click="takePhoto">
            <view class="capture-circle">
              <view class="capture-inner"></view>
            </view>
          </button>
          
          <button class="control-btn switch-btn" @click="switchCamera">
            <image class="switch-icon" src="../../static/DrugScan/switch-camera.svg" mode="aspectFit" />
            <text class="btn-text">切换</text>
          </button>
        </view>
      </view>
      
      <!-- 拍摄提示 -->
      <view class="camera-tips">
        <text class="tip-text">将药品置于框内，确保光线充足</text>
      </view>
    </view>

    <!-- 识别中界面 -->
    <view class="scanning-container" v-if="pageState === 'scanning'">
      <view class="scanning-content">
        <!-- 扫描动画 -->
        <view class="scanning-animation">
          <view class="scan-line"></view>
          <view class="scan-grid">
            <view class="grid-line horizontal"></view>
            <view class="grid-line vertical"></view>
            <!-- 四个角 -->
            <view class="corner corner-top-left"></view>
            <view class="corner corner-top-right"></view>
            <view class="corner corner-bottom-left"></view>
            <view class="corner corner-bottom-right"></view>
          </view>
        </view>
        
        <!-- 识别进度 -->
        <view class="scanning-progress">
          <view class="progress-text">
            <text class="text-title">识别中</text>
            <text class="text-subtitle">正在分析药品信息...</text>
          </view>
          <view class="progress-bar">
            <view class="progress-track">
              <view 
                class="progress-fill" 
                :style="{ width: progress + '%' }"
              ></view>
            </view>
            <text class="progress-percent">{{ Math.round(progress) }}%</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 识别结果界面 -->
    <view class="result-container" v-if="pageState === 'result'">
      <scroll-view 
        class="result-content"
        scroll-y
        :show-scrollbar="false"
        :scroll-x="false"
      >
        <!-- 药品照片 -->
        <view class="drug-image-section">
          <view class="section-header">
            <text class="section-title">药品照片</text>
          </view>
          <view class="drug-image-card">
            <image 
              class="drug-image" 
              :src="drugInfo.image" 
              mode="aspectFit"
              @click="previewImage"
            />
            <button class="re-capture-btn" @click="reCapture">
              <image class="re-capture-icon" src="../../static/DrugScan/refresh.svg" mode="aspectFit" />
              <text class="re-capture-text">重拍</text>
            </button>
          </view>
        </view>

        <!-- 药品基本信息 -->
        <view class="basic-info-section">
          <view class="drug-icon">💊</view>
          <text class="drug-name">{{ drugInfo.name }}</text>
          <text class="drug-spec">{{ drugInfo.specification }}</text>
          
          <view class="confidence-badge">
            <image class="confidence-icon" src="../../static/DrugScan/confidence.svg" mode="aspectFit" />
            <text class="confidence-text">识别置信度 {{ drugInfo.confidence }}%</text>
          </view>
        </view>

        <!-- 自动识别用法 -->
        <view class="usage-section">
          <view class="section-header">
            <image class="section-icon" src="../../static/DrugScan/note.svg" mode="aspectFit" />
            <text class="section-title">自动识别用法</text>
            <text class="section-note">(AI识别结果，仅供参考)</text>
          </view>
          
          <view class="usage-card">
            <view class="usage-item" v-for="(item, index) in drugInfo.usage" :key="index">
              <image class="usage-icon" :src="item.icon" mode="aspectFit" />
              <view class="usage-content">
                <text class="usage-label">{{ item.label }}</text>
                <text class="usage-value">{{ item.value }}</text>
              </view>
              <button 
                class="edit-btn" 
                v-if="item.editable"
                @click="editUsage(item.type)"
              >
                <text class="edit-text">编辑</text>
              </button>
            </view>
          </view>
        </view>

        <!-- 不良反应提示 -->
        <view class="warning-section" v-if="drugInfo.warnings.length > 0">
          <view class="section-header">
            <image class="section-icon" src="../../static/Home/warning.svg" mode="aspectFit" />
            <text class="section-title">不良反应提示</text>
          </view>
          
          <view class="warning-card">
            <view class="warning-item" v-for="(warning, index) in drugInfo.warnings" :key="index">
              <view class="warning-icon">⚠️</view>
              <view class="warning-content">
                <text class="warning-text">{{ warning }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 药品详情 -->
        <view class="detail-section" v-if="drugInfo.details.length > 0">
          <view class="section-header">
            <image class="section-icon" src="../../static/Mine/info.svg" mode="aspectFit" />
            <text class="section-title">药品详情</text>
          </view>
          
          <view class="detail-card">
            <view 
              class="detail-item"
              v-for="(detail, index) in drugInfo.details"
              :key="index"
            >
              <text class="detail-label">{{ detail.label }}</text>
              <text class="detail-value">{{ detail.value }}</text>
            </view>
          </view>
        </view>
      </scroll-view>

      <!-- 底部操作按钮 -->
      <view class="action-buttons">
        <button class="action-btn rescan-btn" @click="restartScan">
          <image class="btn-icon" src="../../static/DrugScan/refresh.svg" mode="aspectFit" />
          <text class="btn-text">重新识别</text>
        </button>
        <button class="action-btn add-btn" @click="addToMedicineBox">
          <image class="btn-icon" src="../../static/Health/plus-circle.svg" mode="aspectFit" />
          <text class="btn-text">添加到药箱</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      pageState: 'camera', // 'camera', 'scanning', 'result'
      flashOn: false,
      cameraDevice: 'back',
      showFocus: false,
      focusStyle: {},
      progress: 0,
      drugInfo: {
        image: '/static/drugs/aspirin.jpg',
        name: '阿司匹林肠溶片',
        specification: '100mg × 30片',
        confidence: 92,
        usage: [
          {
            type: 'frequency',
            label: '服用频率',
            value: '每日 1 次',
            icon: '../../static/DrugScan/frequency.svg',
            editable: true
          },
          {
            type: 'dosage',
            label: '每次剂量',
            value: '每次 1 片',
            icon: '../../static/DrugScan/dosage.svg',
            editable: true
          },
          {
            type: 'time',
            label: '服用时间',
            value: '饭后服用',
            icon: '../../static/DrugScan/time.svg',
            editable: true
          },
          {
            type: 'reminder',
            label: '提醒时间',
            value: '早上 10:00',
            icon: '../../static/DrugScan/reminder.svg',
            editable: true
          }
        ],
        warnings: [
          '可能出现胃肠道不适、恶心、呕吐',
          '长期服用需定期检查肝功能',
          '与抗凝血药物同用时需谨慎',
          '孕妇及哺乳期妇女禁用'
        ],
        details: [
          { label: '药品类型', value: '处方药' },
          { label: '生产企业', value: '拜耳医药保健有限公司' },
          { label: '批准文号', value: '国药准字H20000001' },
          { label: '存储条件', value: '避光、密封、阴凉处保存' },
          { label: '有效期', value: '36个月' }
        ]
      }
    }
  },
  onLoad() {
    // 初始化相机权限
    this.initCamera()
  },
  onUnload() {
    // 清理资源
    this.stopScanning()
  },
  methods: {
    initCamera() {
      // 检查相机权限
      uni.authorize({
        scope: 'scope.camera',
        success: () => {
          console.log('相机授权成功')
        },
        fail: () => {
          uni.showModal({
            title: '需要相机权限',
            content: '请允许使用相机进行药物识别',
            success: (res) => {
              if (res.confirm) {
                uni.openSetting()
              } else {
                uni.navigateBack()
              }
            }
          })
        }
      })
    },
    cameraError(e) {
      console.error('相机错误:', e.detail)
      uni.showToast({
        title: '相机启动失败',
        icon: 'error'
      })
    },
    toggleFlash() {
      this.flashOn = !this.flashOn
      if (this.$refs.camera) {
        this.$refs.camera.flash = this.flashOn ? 'on' : 'off'
      }
    },
    switchCamera() {
      this.cameraDevice = this.cameraDevice === 'back' ? 'front' : 'back'
      if (this.$refs.camera) {
        this.$refs.camera.devicePosition = this.cameraDevice
      }
    },
    takePhoto() {
      if (!this.$refs.camera) return
      
      this.$refs.camera.takePhoto({
        quality: 'high',
        success: (res) => {
          this.startScanning(res.tempImagePath)
        },
        fail: (err) => {
          console.error('拍照失败:', err)
          uni.showToast({
            title: '拍照失败',
            icon: 'error'
          })
        }
      })
    },
    chooseImage() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album'],
        success: (res) => {
          this.startScanning(res.tempFilePaths[0])
        },
        fail: (err) => {
          console.error('选择图片失败:', err)
        }
      })
    },
    startScanning(imagePath) {
      // 保存图片路径
      this.drugInfo.image = imagePath
      
      // 切换到扫描状态
      this.pageState = 'scanning'
      
      // 开始扫描动画
      this.startProgressAnimation()
      
      // 模拟识别过程
      setTimeout(() => {
        this.pageState = 'result'
      }, 3000)
    },
    startProgressAnimation() {
      this.progress = 0
      const interval = setInterval(() => {
        this.progress += 10
        if (this.progress >= 100) {
          clearInterval(interval)
        }
      }, 200)
    },
    stopScanning() {
      this.progress = 0
    },
    restartScan() {
      this.pageState = 'camera'
      this.progress = 0
    },
    reCapture() {
      this.pageState = 'camera'
    },
    previewImage() {
      uni.previewImage({
        urls: [this.drugInfo.image],
        current: 0
      })
    },
    editUsage(type) {
      const usage = this.drugInfo.usage.find(item => item.type === type)
      if (!usage) return
      
      uni.showModal({
        title: `编辑${usage.label}`,
        content: '请输入新的值',
        editable: true,
        placeholderText: usage.value,
        success: (res) => {
          if (res.confirm && res.content) {
            usage.value = res.content
            uni.showToast({
              title: '修改成功',
              icon: 'success'
            })
          }
        }
      })
    },
    addToMedicineBox() {
      uni.showLoading({
        title: '添加中...'
      })
      
      // 模拟添加过程
      setTimeout(() => {
        uni.hideLoading()
        uni.showToast({
          title: '添加成功',
          icon: 'success'
        })
        
        // 延迟返回上一页
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      }, 1000)
    },
    goBack() {
      if (this.pageState === 'result') {
        uni.showModal({
          title: '确认返回',
          content: '识别结果尚未保存，确定返回吗？',
          success: (res) => {
            if (res.confirm) {
              uni.navigateBack()
            }
          }
        })
      } else {
        uni.navigateBack()
      }
    }
  }
}
</script>

<style scoped lang="scss">
.page-container {
  min-height: 100vh;
  background: #000000;
}

/* 头部导航 */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32rpx;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
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
  transition: all 0.3s ease;
}

.back-btn:active {
  background: rgba(255, 255, 255, 0.3);
}

.back-icon {
  width: 32rpx;
  height: 32rpx;
}

.page-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
}

.header-right {
  display: flex;
  align-items: center;
}

.flash-btn {
  width: 64rpx;
  height: 64rpx;
  padding: 0;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.flash-btn:active {
  background: rgba(255, 255, 255, 0.3);
}

.flash-icon {
  width: 32rpx;
  height: 32rpx;
}

/* 相机界面 */
.camera-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;
}

.camera {
  width: 100%;
  height: 100vh;
}

.focus-area {
  position: absolute;
  width: 200rpx;
  height: 200rpx;
  border: 4rpx solid #4d8eff;
  border-radius: 20rpx;
  animation: focusPulse 2s infinite;
}

@keyframes focusPulse {
  0% { opacity: 0.8; }
  50% { opacity: 0.3; }
  100% { opacity: 0.8; }
}

.camera-controls {
  position: absolute;
  bottom: 120rpx;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
}

.control-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80%;
  max-width: 600rpx;
}

.control-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  padding: 0;
  
  &.gallery-btn,
  &.switch-btn {
    max-width: 120rpx;
  }
}

.gallery-icon,
.switch-icon {
  width: 48rpx;
  height: 48rpx;
  margin-bottom: 8rpx;
}

.btn-text {
  font-size: 24rpx;
  color: #ffffff;
  font-weight: 500;
}

.capture-btn {
  width: 120rpx;
  height: 120rpx;
}

.capture-circle {
  width: 100rpx;
  height: 100rpx;
  border: 8rpx solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.capture-btn:active .capture-circle {
  border-color: rgba(77, 142, 255, 0.8);
  transform: scale(0.95);
}

.capture-inner {
  width: 80rpx;
  height: 80rpx;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 0 20rpx rgba(77, 142, 255, 0.5);
}

.camera-tips {
  position: absolute;
  bottom: 60rpx;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
}

.tip-text {
  font-size: 28rpx;
  color: #ffffff;
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.5);
  text-align: center;
  opacity: 0.9;
}

/* 识别中界面 */
.scanning-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scanning-content {
  width: 80%;
  max-width: 600rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.scanning-animation {
  width: 400rpx;
  height: 400rpx;
  position: relative;
  margin-bottom: 80rpx;
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4rpx;
  background: linear-gradient(90deg, transparent, #4d8eff, transparent);
  animation: scanMove 2s infinite linear;
}

@keyframes scanMove {
  0% { top: 0; }
  100% { top: 400rpx; }
}

.scan-grid {
  width: 100%;
  height: 100%;
  position: relative;
  border: 2rpx solid rgba(77, 142, 255, 0.3);
  border-radius: 32rpx;
}

.grid-line {
  position: absolute;
  background: rgba(77, 142, 255, 0.2);
  
  &.horizontal {
    top: 50%;
    left: 20rpx;
    right: 20rpx;
    height: 2rpx;
    transform: translateY(-1rpx);
  }
  
  &.vertical {
    left: 50%;
    top: 20rpx;
    bottom: 20rpx;
    width: 2rpx;
    transform: translateX(-1rpx);
  }
}

.corner {
  position: absolute;
  width: 40rpx;
  height: 40rpx;
  border-color: #4d8eff;
  
  &.corner-top-left {
    top: 0;
    left: 0;
    border-top: 6rpx solid;
    border-left: 6rpx solid;
    border-top-left-radius: 32rpx;
  }
  
  &.corner-top-right {
    top: 0;
    right: 0;
    border-top: 6rpx solid;
    border-right: 6rpx solid;
    border-top-right-radius: 32rpx;
  }
  
  &.corner-bottom-left {
    bottom: 0;
    left: 0;
    border-bottom: 6rpx solid;
    border-left: 6rpx solid;
    border-bottom-left-radius: 32rpx;
  }
  
  &.corner-bottom-right {
    bottom: 0;
    right: 0;
    border-bottom: 6rpx solid;
    border-right: 6rpx solid;
    border-bottom-right-radius: 32rpx;
  }
}

.scanning-progress {
  width: 100%;
}

.progress-text {
  text-align: center;
  margin-bottom: 40rpx;
}

.text-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 12rpx;
}

.text-subtitle {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.progress-bar {
  width: 100%;
}

.progress-track {
  height: 12rpx;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 6rpx;
  transition: width 0.3s ease;
}

.progress-percent {
  display: block;
  text-align: center;
  font-size: 28rpx;
  color: #4d8eff;
  font-weight: 600;
}

/* 识别结果界面 */
.result-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8faff 0%, #ffffff 100%);
  padding-top: 120rpx;
  padding-bottom: 200rpx;
}

.result-content {
  height: 100%;
  padding: 32rpx;
  box-sizing: border-box;
  overflow-x: hidden !important;
}

.result-content::-webkit-scrollbar {
  display: none;
}

/* 药品照片区域 */
.drug-image-section {
  margin-bottom: 40rpx;
}

.section-header {
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2d3b4e;
}

.drug-image-card {
  position: relative;
  background: #ffffff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.drug-image {
  width: 100%;
  height: 400rpx;
  display: block;
}

.re-capture-btn {
  position: absolute;
  bottom: 20rpx;
  right: 20rpx;
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10rpx);
  border-radius: 20rpx;
  border: none;
  transition: all 0.3s ease;
}

.re-capture-btn:active {
  background: rgba(0, 0, 0, 0.8);
}

.re-capture-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 8rpx;
}

.re-capture-text {
  font-size: 24rpx;
  color: #ffffff;
  font-weight: 500;
}

/* 药品基本信息 */
.basic-info-section {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
  text-align: center;
}

.drug-icon {
  font-size: 60rpx;
  margin-bottom: 20rpx;
}

.drug-name {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #2d3b4e;
  margin-bottom: 16rpx;
}

.drug-spec {
  display: block;
  font-size: 28rpx;
  color: #555e6d;
  margin-bottom: 24rpx;
}

.confidence-badge {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 20rpx;
}

.confidence-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 8rpx;
}

.confidence-text {
  font-size: 24rpx;
  color: #10b981;
  font-weight: 500;
}

/* 用法区域 */
.usage-section {
  margin-bottom: 40rpx;
}

.usage-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.usage-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  
  &:not(:last-child) {
    border-bottom: 2rpx solid rgba(77, 142, 255, 0.1);
  }
}

.usage-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.usage-content {
  flex: 1;
}

.usage-label {
  display: block;
  font-size: 26rpx;
  color: #87909c;
  margin-bottom: 8rpx;
}

.usage-value {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
}

.edit-btn {
  padding: 12rpx 24rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 20rpx;
  border: none;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.edit-btn:active {
  background: rgba(77, 142, 255, 0.2);
}

.edit-text {
  font-size: 24rpx;
  color: #4d8eff;
  font-weight: 500;
}

/* 警告区域 */
.warning-section {
  margin-bottom: 40rpx;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 12rpx;
}

.section-note {
  font-size: 24rpx;
  color: #87909c;
  margin-left: 12rpx;
}

.warning-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(245, 158, 11, 0.2);
}

.warning-item {
  display: flex;
  margin-bottom: 20rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.warning-icon {
  font-size: 32rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
}

.warning-text {
  font-size: 28rpx;
  color: #555e6d;
  line-height: 1.4;
}

/* 详情区域 */
.detail-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 0;
  
  &:not(:last-child) {
    border-bottom: 2rpx solid rgba(77, 142, 255, 0.1);
  }
}

.detail-label {
  font-size: 28rpx;
  color: #87909c;
}

.detail-value {
  font-size: 28rpx;
  font-weight: 500;
  color: #2d3b4e;
  text-align: right;
  max-width: 60%;
}

/* 底部操作按钮 */
.action-buttons {
  position: fixed;
  bottom: 120rpx;
  left: 0;
  right: 0;
  display: flex;
  padding: 0 32rpx;
  gap: 24rpx;
  z-index: 100;
}

.action-btn {
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
  transition: all 0.3s ease;
  
  &:active {
    transform: scale(0.98);
  }
}

.rescan-btn {
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 100%);
  border: 2rpx solid rgba(77, 142, 255, 0.2);
  box-shadow: 0 4rpx 16rpx rgba(45, 107, 255, 0.1);
  
  .btn-icon {
    width: 32rpx;
    height: 32rpx;
    margin-right: 12rpx;
  }
  
  .btn-text {
    color: #4d8eff;
  }
}

.add-btn {
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  box-shadow: 0 8rpx 32rpx rgba(45, 107, 255, 0.3);
  
  .btn-icon {
    width: 32rpx;
    height: 32rpx;
    margin-right: 12rpx;
  }
  
  .btn-text {
    color: #ffffff;
  }
}
</style>