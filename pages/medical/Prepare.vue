<!-- pages/medical/prepare.vue -->
<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header">
      <view class="padding"></view>
      <view class="header-bottom">
        <view class="header-left">
          <view class="back-btn" @click="goBack">
            <image class="back-icon" src="/static/Register/back.png" mode="aspectFit" />
          </view>
          <text class="page-title">就医准备单</text>
        </view>
        <view class="header-right">
          <image class="icon" src="/static/Prepare/share.svg" />
          <image class="icon" src="/static/Mine/export.svg" />
        </view>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view class="main-content" scroll-y :show-scrollbar="false" :scroll-x="false">
      <!-- 基本信息 -->
      <view class="basic-info">
        <view class="info-row">
          <image class="info-icon" src="/static/Health/time.svg" mode="aspectFit" />
          <text class="info-label">生成时间：</text>
          <text class="info-value">{{ documentInfo.generatedTime }}</text>
        </view>
        <view class="info-row">
          <image class="info-icon" src="/static/Prepare/department.svg" mode="aspectFit" />
          <text class="info-label">就诊科室：</text>
          <picker mode="selector" :range="departments" :value="departmentIndex" @change="onDepartmentChange">
            <view class="info-value picker-value">
              <text>{{ documentInfo.department }}</text>
              <image class="picker-arrow" src="/static/Health/down.svg" mode="aspectFit" />
            </view>
          </picker>
        </view>
        <view class="info-row">
          <image class="info-icon" src="/static/Login/user.png" mode="aspectFit" />
          <text class="info-label">患者：</text>
          <text class="info-value">{{ documentInfo.patient }}</text>
        </view>
        <view class="info-row">
          <image class="info-icon" src="/static/Health/calendar.svg" mode="aspectFit" />
          <text class="info-label">就诊日期：</text>
          <picker mode="date" :value="visitDate" @change="onDateChange">
            <view class="info-value picker-value">
              <text>{{ documentInfo.visitDate }}</text>
              <image class="picker-arrow" src="/static/Health/down.svg" mode="aspectFit" />
            </view>
          </picker>
        </view>
      </view>

      <!-- 近期用药清单 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-container">
            <image class="section-icon" src="/static/Home/medical-list.svg" mode="aspectFit" />
            <text class="section-title">近期用药清单</text>
          </view>
          <button class="edit-section-btn" @click="editMedications">
            <image class="edit-icon" src="/static/Prepare/edit.svg" mode="aspectFit" />
          </button>
        </view>

        <view class="medication-list">
          <view class="medication-item" v-for="med in documentInfo.medications" :key="med.id"
            @click="toMedicationDetail(med.id)">
            <view class="medication-header">
              <image class="pill-icon" src="/static/Health/pill-active.svg" mode="aspectFit" />
              <text class="medication-name">{{ med.name }}</text>
              <view class="medication-status" :class="med.statusClass">
                <text>{{ med.status }}</text>
              </view>
            </view>
            <view class="medication-details">
              <view class="detail-item">
                <image class="detail-icon" src="/static/Prepare/schedule.svg" mode="aspectFit" />
                <text class="detail-text">{{ med.schedule }}</text>
              </view>
              <view class="detail-item">
                <image class="detail-icon" src="/static/Health/clock-history.svg" mode="aspectFit" />
                <text class="detail-text">已服用{{ med.takenDays }}天</text>
                <view class="missed-count" v-if="med.missedCount > 0">
                  <text class="missed-text">⚠️漏服{{ med.missedCount }}次</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="section-footer">
          <text class="footer-text">共 {{ documentInfo.medications.length }} 种药品</text>
        </view>
      </view>

      <!-- 异常健康数据 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-container">
            <image class="section-icon" src="/static/Home/warning.svg" mode="aspectFit" />
            <text class="section-title">异常健康数据</text>
          </view>
          <button class="edit-section-btn" @click="editHealthData">
            <image class="edit-icon" src="/static/Prepare/edit.svg" mode="aspectFit" />
          </button>
        </view>

        <view class="health-data-list">
          <view class="health-data-item" v-for="data in documentInfo.healthData" :key="data.id">
            <view class="data-header">
              <text class="data-date">{{ data.date }}</text>
              <view class="data-indicator">
                <image class="indicator-icon" :src="getIndicatorIcon(data.type)" mode="aspectFit" />
                <text class="indicator-name">{{ data.indicator }}</text>
              </view>
            </view>
            <view class="data-value">
              <text class="value-number">{{ data.value }}</text>
              <text class="value-unit">{{ data.unit }}</text>
              <view class="data-status" :class="data.statusClass">
                <text>{{ data.status }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="section-footer">
          <text class="footer-text">最近7天异常数据</text>
        </view>
      </view>

      <!-- 待咨询问题 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-container">
            <image class="section-icon" src="/static/Prepare/question.svg" mode="aspectFit" />
            <text class="section-title">待咨询问题</text>
          </view>
          <button class="edit-section-btn" @click="editQuestions">
            <image class="edit-icon" src="/static/Prepare/edit.svg" mode="aspectFit" />
          </button>
        </view>

        <view class="questions-list">
          <view class="question-item" v-for="(question, index) in documentInfo.questions" :key="index">
            <text class="question-index">•</text>
            <text class="question-text">{{ question }}</text>
          </view>
        </view>

        <view class="questions-input" v-if="showQuestionInput">
          <textarea class="question-textarea" placeholder="请输入您想问医生的问题..." placeholder-class="textarea-placeholder"
            v-model="newQuestion" maxlength="200" auto-height />
          <view class="textarea-actions">
            <text class="char-count">{{ newQuestion.length }}/200</text>
            <button class="cancel-btn" @click="cancelAddQuestion">取消</button>
            <button class="add-btn" @click="addQuestion" :disabled="!newQuestion.trim()">添加</button>
          </view>
        </view>

        <button class="add-question-btn" @click="showAddQuestion" v-if="!showQuestionInput">
          <image class="add-icon" src="/static/Health/plus-circle.svg" mode="aspectFit" />
          <text class="add-text">添加问题</text>
        </button>
      </view>

      <!-- 其他信息 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-container">
            <image class="section-icon" src="/static/DrugScan/note.svg" mode="aspectFit" />
            <text class="section-title">其他信息</text>
          </view>
        </view>

        <view class="other-info">
          <textarea class="other-textarea" placeholder="其他需要告知医生的信息，如过敏史、既往病史等..."
            placeholder-class="textarea-placeholder" v-model="documentInfo.otherInfo" maxlength="500" auto-height />
          <text class="char-count">{{ documentInfo.otherInfo.length }}/500</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部操作按钮 -->
    <view class="action-buttons">
      <button class="action-btn edit-btn" @click="editContent">
        <image class="btn-icon" src="/static/Prepare/edit.svg" mode="aspectFit" />
        <text class="btn-text">编辑内容</text>
      </button>
      <button class="action-btn generate-btn" @click="generatePDF">
        <image class="btn-icon" src="/static/Prepare/pdf.svg" mode="aspectFit" />
        <text class="btn-text">生成PDF</text>
      </button>
    </view>
  </view>
</template>

<script>
import { BASE_URL } from '../../config/config';

export default {
  data() {
    return {
      refreshing: false,
      departmentIndex: 0,
      visitDate: '2025-12-31',
      showQuestionInput: false,
      newQuestion: '',
      departments: ['心内科', '神经内科', '消化内科', '内分泌科', '普通内科', '其他'],
      documentInfo: {
        generatedTime: '2025/12/31 10:30',
        department: '心内科',
        patient: '张三',
        visitDate: '2025/12/31',
        medications: [
          {
            id: 1,
            name: '阿司匹林肠溶片',
            schedule: '每日1次',
            takenDays: 7,
            missedCount: 0,
            status: '按时服用',
            statusClass: 'status-normal'
          },
          {
            id: 2,
            name: '二甲双胍缓释片',
            schedule: '每日2次',
            takenDays: 3,
            missedCount: 1,
            status: '有漏服',
            statusClass: 'status-warning'
          },
          {
            id: 3,
            name: '阿托伐他汀钙片',
            schedule: '每晚1次',
            takenDays: 15,
            missedCount: 0,
            status: '按时服用',
            statusClass: 'status-normal'
          }
        ],
        healthData: [
          {
            id: 1,
            date: '12/30',
            type: 'bloodPressure',
            indicator: '血压',
            value: '145/90',
            unit: 'mmHg',
            status: '偏高',
            statusClass: 'status-danger'
          },
          {
            id: 2,
            date: '12/28',
            type: 'heartRate',
            indicator: '心率',
            value: '95',
            unit: 'bpm',
            status: '偏快',
            statusClass: 'status-warning'
          },
          {
            id: 3,
            date: '12/27',
            type: 'bloodPressure',
            indicator: '血压',
            value: '142/88',
            unit: 'mmHg',
            status: '偏高',
            statusClass: 'status-danger'
          }
        ],
        questions: [
          '血压持续偏高是否需要调整降压药剂量？',
          '阿司匹林能和XX药一起吃吗？',
          '最近头晕是药物副作用吗？',
          '是否需要调整阿司匹林的服用时间？'
        ],
        otherInfo: ''
      }
    }
  },
  onLoad(options) {
    if (options.dept) {
      const index = this.departments.findIndex(dept => dept === options.dept)
      if (index !== -1) {
        this.departmentIndex = index
        this.documentInfo.department = this.departments[index]
      }
    }
  },
  methods: {
    onRefresh() {
      this.refreshing = true
      // 模拟刷新数据
      setTimeout(() => {
        this.refreshing = false
        uni.showToast({
          title: '已更新',
          icon: 'success'
        })
      }, 1000)
    },
    goBack() {
      uni.navigateBack()
    },
    onDepartmentChange(e) {
      this.departmentIndex = e.detail.value
      this.documentInfo.department = this.departments[this.departmentIndex]
    },
    onDateChange(e) {
      this.visitDate = e.detail.value
      // 格式化日期
      const date = new Date(e.detail.value)
      this.documentInfo.visitDate = `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
    },
    editMedications() {
      uni.navigateTo({
        url: '/pages/medical/edit-medications'
      })
    },
    editHealthData() {
      uni.navigateTo({
        url: '/pages/medical/edit-health-data'
      })
    },
    editQuestions() {
      uni.showActionSheet({
        itemList: ['编辑问题', '清空问题'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.showAddQuestion()
          } else if (res.tapIndex === 1) {
            this.clearQuestions()
          }
        }
      })
    },
    showAddQuestion() {
      this.showQuestionInput = true
      this.newQuestion = ''
    },
    addQuestion() {
      if (this.newQuestion.trim()) {
        this.documentInfo.questions.push(this.newQuestion.trim())
        this.newQuestion = ''
        this.showQuestionInput = false
        uni.showToast({
          title: '问题已添加',
          icon: 'success'
        })
      }
    },
    cancelAddQuestion() {
      this.showQuestionInput = false
      this.newQuestion = ''
    },
    clearQuestions() {
      uni.showModal({
        title: '清空问题',
        content: '确定要清空所有待咨询问题吗？',
        success: (res) => {
          if (res.confirm) {
            this.documentInfo.questions = []
            uni.showToast({
              title: '已清空',
              icon: 'success'
            })
          }
        }
      })
    },
    toMedicationDetail(id) {
      uni.navigateTo({
        url: `/pages/medication/detail?id=${id}`
      })
    },
    getIndicatorIcon(type) {
      const icons = {
        bloodPressure: '/static/Prepare/blood-pressure.svg',
        heartRate: '/static/Home/heart.svg',
        bloodSugar: '/static/Prepare/blood-sugar.svg',
        weight: '/static/Prepare/weight.svg'
      }
      return icons[type] || '/static/Prepare/health.svg'
    },
    editContent() {
      uni.navigateTo({
        url: '/pages/medical/edit-document'
      })
    },
    shareDocument() {
      uni.showActionSheet({
        itemList: ['分享给微信好友', '分享到朋友圈', '复制链接', '生成分享图'],
        success: (res) => {
          const actions = [
            this.shareToWechat,
            this.shareToTimeline,
            this.copyLink,
            this.generateShareImage
          ]
          if (actions[res.tapIndex]) {
            actions[res.tapIndex]()
          }
        }
      })
    },
    shareToWechat() {
      uni.showToast({
        title: '准备分享到微信...',
        icon: 'none'
      })
    },
    shareToTimeline() {
      uni.showToast({
        title: '准备分享到朋友圈...',
        icon: 'none'
      })
    },
    copyLink() {
      uni.setClipboardData({
        data: 'https://medical-prepare.com/share/123456',
        success: () => {
          uni.showToast({
            title: '链接已复制',
            icon: 'success'
          })
        }
      })
    },
    generateShareImage() {
      uni.showLoading({
        title: '生成分享图中...'
      })

      setTimeout(() => {
        uni.hideLoading()
        uni.showToast({
          title: '分享图已生成',
          icon: 'success'
        })
      }, 1500)
    },
    exportDocument() {
      this.generatePDF()
    },
    async generatePDF() {
      try {
        uni.showLoading({ title: '生成PDF中...' })

        const payload = {
          generatedTime: this.documentInfo.generatedTime || '',
          department: this.documentInfo.department || '',
          patient: this.documentInfo.patient || '',
          visitDate: this.documentInfo.visitDate || '',
          medications: this.documentInfo.medications || [],
          healthData: this.documentInfo.healthData || [],
          questions: this.documentInfo.questions || [],
          otherInfo: this.documentInfo.otherInfo || ''
        }


        const accessToken = uni.getStorageSync('accessToken') || ''

        const res = await uni.request({
          url: `${BASE_URL}/medical/prepare/pdf`,
          method: 'POST',
          data: payload,
          header: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
          }
        })

        uni.hideLoading()

        if (!res || res.statusCode !== 200) {
          uni.showModal({
            title: '生成失败',
            content: `请求失败(${res?.statusCode || ''})`,
            showCancel: false
          })
          return
        }

        const body = res.data || {}
        // 兼容：{success,fileUrl} 或 {code,data.fileUrl}
        const success = body.success === true || body.code === 0
        const fileUrlRaw = body.fileUrl || body?.data?.fileUrl

        if (!success || !fileUrlRaw) {
          uni.showModal({
            title: '生成失败',
            content: body.message || '后端未返回文件地址',
            showCancel: false
          })
          return
        }

        // 拼完整URL（后端可能返回相对路径）
        const fileUrl = /^https?:\/\//.test(fileUrlRaw) ? fileUrlRaw : `${BASE_URL}${fileUrlRaw}`

        uni.showModal({
          title: 'PDF生成成功',
          content: '是否现在查看？',
          confirmText: '查看',
          cancelText: '稍后',
          success: async (r) => {
            if (!r.confirm) return

            const accessToken = uni.getStorageSync('accessToken') || ''

            // #ifdef H5
            try {
              const resp = await fetch(fileUrl, {
                method: 'GET',
                headers: {
                  Authorization: `Bearer ${accessToken}`
                }
              })

              // token 失效
              if (resp.status === 401 || resp.status === 402) {
                uni.removeStorageSync('accessToken')
                uni.showModal({
                  title: '登录已过期',
                  content: '请重新登录后再试',
                  showCancel: false,
                  success: () => uni.reLaunch({ url: '/pages/Login' })
                })
                return
              }

              if (!resp.ok) {
                uni.showToast({ title: '打开失败', icon: 'none' })
                return
              }

              const blob = await resp.blob()
              const blobUrl = window.URL.createObjectURL(blob)
              window.open(blobUrl, '_blank')
            } catch (e) {
              uni.showToast({ title: '打开失败', icon: 'none' })
            }
            // #endif

            // #ifndef H5
            uni.downloadFile({
              url: fileUrl,
              header: {
                Authorization: `Bearer ${accessToken}`
              },
              success: (d) => {
                if (d.statusCode === 200) {
                  uni.openDocument({
                    filePath: d.tempFilePath,
                    showMenu: true
                  })
                } else if (d.statusCode === 401 || d.statusCode === 402) {
                  uni.removeStorageSync('accessToken')
                  uni.showModal({
                    title: '登录已过期',
                    content: '请重新登录后再试',
                    showCancel: false,
                    success: () => uni.reLaunch({ url: '/pages/Login' })
                  })
                } else {
                  uni.showToast({ title: '下载失败', icon: 'none' })
                }
              },
              fail: () => uni.showToast({ title: '下载失败', icon: 'none' })
            })
            // #endif
          }
        })
      } catch (e) {
        uni.hideLoading()
        uni.showModal({
          title: '生成失败',
          content: e?.message || '网络异常，请稍后重试',
          showCancel: false
        })
      }
    }
  }
}
</script>

<style scoped lang="scss">
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8faff 0%, #ffffff 100%);
  padding-bottom: 200rpx;
}

/* 头部导航栏 */
.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 32rpx 24rpx;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 0 0 32rpx 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.15);
}

.padding {
  height: 64rpx;
  /* 顶部留白，适配状态栏 */
}

.header-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
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
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.icon {
  width: 48rpx;
  height: 48rpx;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;

  height: 64rpx;
  width: 64rpx;
  background: transparent;
}

.share-icon,
.export-icon {
  width: 48rpx;
  height: 48rpx;
}

/* 主要内容区域 */
.main-content {
  height: 100%;
  padding: 32rpx;
  box-sizing: border-box;
  overflow-x: hidden !important;
}

.main-content::-webkit-scrollbar {
  display: none;
}

/* 基本信息 */
.basic-info {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.info-label {
  font-size: 28rpx;
  color: #87909c;
  min-width: 140rpx;
}

.info-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #2d3b4e;
  flex: 1;
}

.picker-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 20rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 16rpx;
  transition: all 0.3s ease;

  &:active {
    background: rgba(77, 142, 255, 0.2);
  }
}

.picker-arrow {
  width: 20rpx;
  height: 20rpx;
  margin-left: 8rpx;
}

/* 分区样式 */
.section {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
}

.section-title-container {
  display: flex;
  align-items: center;
}

.section-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 16rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2d3b4e;
}

.edit-section-btn {
  display: flex;
  align-items: center;
  justify-content: center;

  height: 64rpx;
  width: 64rpx;

  padding: 0;

  margin-left: auto;
  margin-right: 0;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 32rpx;
  border: #2d6bff solid 2rpx;
}

.edit-icon {
  width: 48rpx;
  height: 48rpx;

  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}

.edit-text {
  font-size: 24rpx;
  color: #4d8eff;
  font-weight: 500;
}

/* 药品清单 */
.medication-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.medication-item {
  padding: 24rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
  transition: all 0.3s ease;

  &:active {
    background: rgba(77, 142, 255, 0.1);
  }
}

.medication-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.pill-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 16rpx;
}

.medication-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
  flex: 1;
}

.medication-status {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.status-normal {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
  }

  &.status-warning {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }

  &.status-danger {
    background: rgba(255, 107, 107, 0.1);
    color: #ff6b6b;
  }
}

.medication-details {
  padding-left: 48rpx;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 12rpx;
}

.detail-text {
  font-size: 26rpx;
  color: #555e6d;
  flex: 1;
}

.missed-count {
  margin-left: 16rpx;
}

.missed-text {
  font-size: 22rpx;
  color: #ff6b6b;
  font-weight: 500;
}

/* 健康数据 */
.health-data-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.health-data-item {
  padding: 24rpx;
  background: rgba(245, 158, 11, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(245, 158, 11, 0.1);
}

.data-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.data-date {
  font-size: 26rpx;
  font-weight: 600;
  color: #f59e0b;
}

.data-indicator {
  display: flex;
  align-items: center;
}

.indicator-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 8rpx;
}

.indicator-name {
  font-size: 26rpx;
  color: #555e6d;
}

.data-value {
  display: flex;
  align-items: baseline;
}

.value-number {
  font-size: 36rpx;
  font-weight: 700;
  color: #2d3b4e;
  margin-right: 8rpx;
}

.value-unit {
  font-size: 24rpx;
  color: #87909c;
  margin-right: 20rpx;
}

.data-status {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.status-warning {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }

  &.status-danger {
    background: rgba(255, 107, 107, 0.1);
    color: #ff6b6b;
  }
}

/* 待咨询问题 */
.questions-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.question-item {
  display: flex;
  padding: 24rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.question-index {
  font-size: 36rpx;
  color: #4d8eff;
  margin-right: 20rpx;
  line-height: 1;
  align-self: flex-start;
}

.question-text {
  font-size: 28rpx;
  color: #2d3b4e;
  line-height: 1.4;
  flex: 1;
}

.questions-input {
  margin-bottom: 24rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.2);
  overflow: hidden;
}

.question-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #2d3b4e;
  line-height: 1.4;
  background: transparent;
}

.textarea-placeholder {
  color: #b4bfd3;
  font-size: 28rpx;
}

.textarea-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  border-top: 2rpx solid rgba(77, 142, 255, 0.1);
}

.char-count {
  font-size: 24rpx;
  color: #87909c;
}

.cancel-btn,
.add-btn {
  padding: 12rpx 24rpx;
  border-radius: 16rpx;
  border: none;
  font-size: 26rpx;
  font-weight: 500;
  margin-left: 16rpx;
  transition: all 0.3s ease;
}

.cancel-btn {
  background: rgba(180, 191, 211, 0.1);
  color: #87909c;

  &:active {
    background: rgba(180, 191, 211, 0.2);
  }
}

.add-btn {
  background: rgba(77, 142, 255, 0.1);
  color: #4d8eff;

  &:active {
    background: rgba(77, 142, 255, 0.2);
  }

  &[disabled] {
    background: rgba(180, 191, 211, 0.1);
    color: #b4bfd3;
    opacity: 0.6;
  }
}

.add-question-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 28rpx 0;
  background: linear-gradient(135deg, rgba(77, 142, 255, 0.1) 0%, rgba(45, 107, 255, 0.05) 100%);
  border: 2rpx dashed rgba(77, 142, 255, 0.3);
  border-radius: 20rpx;
  transition: all 0.3s ease;

  &:active {
    background: linear-gradient(135deg, rgba(77, 142, 255, 0.15) 0%, rgba(45, 107, 255, 0.1) 100%);
  }
}

.add-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
}

.add-text {
  font-size: 28rpx;
  color: #4d8eff;
  font-weight: 600;
}

/* 其他信息 */
.other-info {
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
  overflow: hidden;
}

.other-textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #2d3b4e;
  line-height: 1.4;
  background: transparent;
}

/* 分区页脚 */
.section-footer {
  padding-top: 16rpx;
  border-top: 2rpx solid rgba(77, 142, 255, 0.1);
}

.footer-text {
  font-size: 24rpx;
  color: #87909c;
  text-align: center;
  display: block;
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
  padding: 0;
  font-size: 28rpx;
  font-weight: 600;
  transition: all 0.3s ease;

  &:active {
    transform: scale(0.98);
  }
}

.edit-btn {
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

.generate-btn {
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