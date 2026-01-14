<!-- pages/health/data.vue -->
<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header">
      <view class="header-left">
        <text class="page-title">健康数据</text>
      </view>
      <view class="header-right">
        <button class="report-btn" @click="generateReport">
          <image class="chart-icon" src="/static/icons/chart.png" mode="aspectFit" />
          <text class="report-text">生成报告</text>
        </button>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view 
      class="main-content"
      scroll-y
      :show-scrollbar="false"
      :scroll-x="false"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 本周概览 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">本周概览</text>
        </view>
        
        <view class="overview-card">
          <view class="overview-item">
            <view class="overview-header">
              <text class="overview-label">服药依从性</text>
              <view class="trend-badge" @click="viewAdherenceDetail">
                <text class="trend-text">📈 较上周 +5%</text>
              </view>
            </view>
            <view class="overview-value">
              <text class="value-number">{{ adherenceRate }}</text>
              <text class="value-unit">%</text>
            </view>
            <!-- 进度条 -->
            <view class="progress-bar">
              <view class="progress-track">
                <view 
                  class="progress-fill" 
                  :style="{ width: adherenceRate + '%' }"
                ></view>
              </view>
              <view class="progress-labels">
                <text class="progress-label">0%</text>
                <text class="progress-label">50%</text>
                <text class="progress-label">100%</text>
              </view>
            </view>
          </view>
          
          <!-- 分隔线 -->
          <view class="divider"></view>
          
          <view class="overview-item">
            <view class="overview-header">
              <text class="overview-label">健康评分</text>
              <view class="score-badge" :class="healthScoreLevel">
                <text class="score-text">{{ getHealthScoreText(healthScore) }}</text>
              </view>
            </view>
            <view class="overview-value">
              <text class="value-number">{{ healthScore }}</text>
              <text class="value-unit">分</text>
            </view>
            <!-- 评分说明 -->
            <view class="score-desc">
              <text class="desc-text">{{ getScoreDescription(healthScore) }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 核心指标 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">核心指标</text>
          <view class="time-selector">
            <picker 
              mode="selector" 
              :range="timeRanges" 
              :value="timeIndex"
              @change="onTimeRangeChange"
            >
              <view class="time-selector-btn">
                <text class="time-text">{{ selectedTimeRange }}</text>
                <image class="dropdown-icon" src="/static/icons/dropdown.png" mode="aspectFit" />
              </view>
            </picker>
          </view>
        </view>
        
        <view class="indicators-grid">
          <view 
            class="indicator-card"
            v-for="indicator in indicators"
            :key="indicator.id"
            @click="viewIndicatorDetail(indicator.id)"
          >
            <view class="indicator-header">
              <image class="indicator-icon" :src="indicator.icon" mode="aspectFit" />
              <text class="indicator-name">{{ indicator.name }}</text>
            </view>
            <view class="indicator-value">
              <text class="value-number">{{ indicator.value }}</text>
              <text class="value-unit">{{ indicator.unit }}</text>
            </view>
            <view class="indicator-status">
              <text class="status-text" :class="indicator.statusClass">
                {{ indicator.status }}
              </text>
              <view class="trend-icon" v-if="indicator.trend">
                <image 
                  :src="getTrendIcon(indicator.trend)" 
                  class="trend-icon-img"
                  mode="aspectFit"
                />
              </view>
            </view>
            <view class="chart-placeholder">
              <text class="chart-icon-mini">📊</text>
              <text class="chart-text">查看趋势</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 健康趋势 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">健康趋势</text>
          <view class="trend-selector">
            <picker 
              mode="selector" 
              :range="trendOptions" 
              :value="trendIndex"
              @change="onTrendChange"
            >
              <view class="trend-selector-btn">
                <text class="trend-text">{{ selectedTrend }}</text>
                <image class="dropdown-icon" src="/static/icons/dropdown.png" mode="aspectFit" />
              </view>
            </picker>
          </view>
        </view>
        
        <view class="trend-card">
          <view class="trend-header">
            <image class="trend-chart-icon" src="/static/icons/line-chart.png" mode="aspectFit" />
            <text class="trend-title">{{ selectedTrend }}趋势({{ selectedTimeRange }})</text>
          </view>
          
          <!-- 简易趋势图（实际项目中可接入echarts等图表库） -->
          <view class="trend-chart">
            <!-- Y轴标签 -->
            <view class="y-axis">
              <text class="y-label">140</text>
              <text class="y-label">120</text>
              <text class="y-label">100</text>
            </view>
            
            <!-- 图表区域 -->
            <view class="chart-area">
              <!-- 网格线 -->
              <view class="grid-lines">
                <view class="grid-line" v-for="i in 4" :key="i"></view>
              </view>
              
              <!-- 数据点 -->
              <view class="data-points">
                <view 
                  class="data-point"
                  :style="{ left: point.x + '%', bottom: point.y + '%' }"
                  v-for="point in trendData"
                  :key="point.id"
                >
                  <view class="point-circle"></view>
                  <view class="point-value">{{ point.value }}</view>
                </view>
              </view>
              
              <!-- 连接线 -->
              <view class="trend-line"></view>
            </view>
            
            <!-- X轴标签 -->
            <view class="x-axis">
              <text class="x-label" v-for="day in xAxisLabels" :key="day">{{ day }}</text>
            </view>
          </view>
          
          <!-- 趋势分析 -->
          <view class="trend-analysis">
            <view class="analysis-item">
              <text class="analysis-label">平均数值:</text>
              <text class="analysis-value">{{ averageValue }}</text>
            </view>
            <view class="analysis-item">
              <text class="analysis-label">变化趋势:</text>
              <text class="analysis-value" :class="trendChangeClass">
                {{ trendChange }}%
              </text>
            </view>
          </view>
        </view>
      </view>

      <!-- 异常提醒 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">异常提醒</text>
          <view class="reminder-count" v-if="alerts.length > 0">
            <text class="count-badge">{{ alerts.length }}</text>
          </view>
        </view>
        
        <view class="alerts-list">
          <view 
            class="alert-card"
            v-for="alert in alerts"
            :key="alert.id"
            :class="alert.level"
            @click="viewAlertDetail(alert.id)"
          >
            <view class="alert-header">
              <image class="alert-icon" :src="getAlertIcon(alert.level)" mode="aspectFit" />
              <view class="alert-info">
                <text class="alert-title">{{ alert.title }}</text>
                <text class="alert-time">{{ alert.time }}</text>
              </view>
              <image 
                class="alert-more" 
                src="/static/icons/right-arrow.png" 
                mode="aspectFit" 
              />
            </view>
            <text class="alert-content">{{ alert.content }}</text>
            <text class="alert-suggestion">{{ alert.suggestion }}</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
    name:"MedicinePage",
  data() {
    return {
      refreshing: false,
      adherenceRate: 92,
      healthScore: 85,
      timeIndex: 0,
      trendIndex: 0,
      timeRanges: ['7天', '30天', '90天'],
      trendOptions: ['血压', '心率', '血糖', '体重'],
      indicators: [
        {
          id: 1,
          name: '血压',
          value: '120/80',
          unit: 'mmHg',
          status: '正常',
          statusClass: 'status-normal',
          trend: 'stable',
          icon: '/static/icons/blood-pressure.png'
        },
        {
          id: 2,
          name: '心率',
          value: '72',
          unit: 'bpm',
          status: '正常',
          statusClass: 'status-normal',
          trend: 'stable',
          icon: '/static/icons/heart-rate.png'
        },
        {
          id: 3,
          name: '血糖',
          value: '5.2',
          unit: 'mmol/L',
          status: '正常',
          statusClass: 'status-normal',
          trend: 'down',
          icon: '/static/icons/blood-sugar.png'
        },
        {
          id: 4,
          name: '体重',
          value: '65',
          unit: 'kg',
          status: '正常',
          statusClass: 'status-normal',
          trend: 'down',
          icon: '/static/icons/weight.png'
        }
      ],
      trendData: [
        { id: 1, x: 0, y: 20, value: '145' },
        { id: 2, x: 25, y: 50, value: '130' },
        { id: 3, x: 50, y: 40, value: '125' },
        { id: 4, x: 75, y: 30, value: '120' },
        { id: 5, x: 100, y: 10, value: '110' }
      ],
      xAxisLabels: ['12/26', '12/27', '12/28', '12/29', '12/30'],
      averageValue: '126 mmHg',
      trendChange: '-5.2',
      alerts: [
        {
          id: 1,
          level: 'warning',
          title: '血压偏高',
          time: '12/30 14:30',
          content: '血压测量值 145/90 mmHg',
          suggestion: '建议减少钠摄入，适量运动'
        },
        {
          id: 2,
          level: 'info',
          title: '服药提醒',
          time: '12/29 10:00',
          content: '阿司匹林漏服1次',
          suggestion: '请按时服药，如需调整请咨询医生'
        }
      ]
    }
  },
  computed: {
    selectedTimeRange() {
      return this.timeRanges[this.timeIndex]
    },
    selectedTrend() {
      return this.trendOptions[this.trendIndex]
    },
    healthScoreLevel() {
      if (this.healthScore >= 90) return 'score-excellent'
      if (this.healthScore >= 80) return 'score-good'
      if (this.healthScore >= 60) return 'score-fair'
      return 'score-poor'
    },
    trendChangeClass() {
      if (this.trendChange.startsWith('-')) return 'trend-down'
      if (this.trendChange.startsWith('+')) return 'trend-up'
      return 'trend-stable'
    }
  },
  onLoad() {
    this.loadHealthData()
  },
  methods: {
    onRefresh() {
      this.refreshing = true
      setTimeout(() => {
        this.refreshing = false
        uni.showToast({
          title: '数据已更新',
          icon: 'success'
        })
      }, 1000)
    },
    loadHealthData() {
      // 加载健康数据
      console.log('加载健康数据')
    },
    generateReport() {
      uni.showLoading({
        title: '生成报告中...'
      })
      
      setTimeout(() => {
        uni.hideLoading()
        uni.navigateTo({
          url: '/pages/health/report'
        })
      }, 1500)
    },
    viewAdherenceDetail() {
      uni.navigateTo({
        url: '/pages/health/adherence'
      })
    },
    getHealthScoreText(score) {
      if (score >= 90) return '优秀'
      if (score >= 80) return '良好'
      if (score >= 60) return '一般'
      return '需关注'
    },
    getScoreDescription(score) {
      if (score >= 90) return '继续保持良好的健康习惯'
      if (score >= 80) return '整体健康状况良好'
      if (score >= 60) return '部分指标需要关注'
      return '建议加强健康管理'
    },
    onTimeRangeChange(e) {
      this.timeIndex = e.detail.value
      this.loadTrendData()
    },
    onTrendChange(e) {
      this.trendIndex = e.detail.value
      this.loadTrendData()
    },
    loadTrendData() {
      // 根据选择的时间范围和指标加载趋势数据
      console.log('加载趋势数据:', this.selectedTrend, this.selectedTimeRange)
    },
    viewIndicatorDetail(id) {
      const indicator = this.indicators.find(i => i.id === id)
      if (indicator) {
        uni.navigateTo({
          url: `/pages/health/indicator-detail?id=${id}&name=${indicator.name}`
        })
      }
    },
    getTrendIcon(trend) {
      const icons = {
        up: '/static/icons/trend-up.png',
        down: '/static/icons/trend-down.png',
        stable: '/static/icons/trend-stable.png'
      }
      return icons[trend] || icons.stable
    },
    viewAlertDetail(id) {
      const alert = this.alerts.find(a => a.id === id)
      if (alert) {
        uni.navigateTo({
          url: `/pages/health/alert-detail?id=${id}`
        })
      }
    },
    getAlertIcon(level) {
      const icons = {
        warning: '/static/icons/warning.png',
        info: '/static/icons/info-circle.png',
        danger: '/static/icons/danger.png'
      }
      return icons[level] || icons.info
    }
  }
}
</script>

<style scoped lang="scss">
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8faff 0%, #ffffff 100%);
  padding-bottom: 120rpx;
}

/* 头部样式 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 32rpx 24rpx;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 0 0 32rpx 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.15);
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1rpx;
}

.header-right {
  display: flex;
  align-items: center;
}

.report-btn {
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 24rpx;
  border: none;
  backdrop-filter: blur(10rpx);
  transition: all 0.3s ease;
}

.report-btn:active {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(0.95);
}

.chart-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 8rpx;
}

.report-text {
  font-size: 26rpx;
  color: #ffffff;
  font-weight: 500;
}

/* 主要内容区域 */
.main-content {
  height: calc(100vh - 240rpx);
  padding: 32rpx;
  box-sizing: border-box;
  overflow-x: hidden !important;
}

.main-content::-webkit-scrollbar {
  display: none;
}

/* 分区样式 */
.section {
  margin-bottom: 40rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2d3b4e;
}

/* 时间选择器 */
.time-selector,
.trend-selector {
  position: relative;
}

.time-selector-btn,
.trend-selector-btn {
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 24rpx;
  transition: all 0.3s ease;
}

.time-selector-btn:active,
.trend-selector-btn:active {
  background: rgba(77, 142, 255, 0.2);
}

.time-text,
.trend-text {
  font-size: 26rpx;
  color: #4d8eff;
  font-weight: 500;
  margin-right: 8rpx;
}

.dropdown-icon {
  width: 20rpx;
  height: 20rpx;
}

/* 提醒计数 */
.reminder-count {
  margin-left: auto;
}

.count-badge {
  display: inline-block;
  padding: 6rpx 16rpx;
  background: #ff6b6b;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 16rpx;
  min-width: 32rpx;
  text-align: center;
}

/* 概览卡片 */
.overview-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.overview-item {
  margin-bottom: 32rpx;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.overview-label {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
}

.trend-badge {
  padding: 8rpx 16rpx;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 20rpx;
}

.trend-text {
  font-size: 24rpx;
  color: #10b981;
  font-weight: 500;
}

.score-badge {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
  
  &.score-excellent {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
  }
  
  &.score-good {
    background: rgba(77, 142, 255, 0.1);
    color: #4d8eff;
  }
  
  &.score-fair {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }
  
  &.score-poor {
    background: rgba(255, 107, 107, 0.1);
    color: #ff6b6b;
  }
}

.score-text {
  font-size: 24rpx;
  font-weight: 500;
}

/* 概览数值 */
.overview-value {
  display: flex;
  align-items: baseline;
  margin-bottom: 24rpx;
}

.value-number {
  font-size: 64rpx;
  font-weight: 700;
  color: #4d8eff;
  line-height: 1;
}

.value-unit {
  font-size: 32rpx;
  color: #87909c;
  margin-left: 8rpx;
}

/* 进度条 */
.progress-bar {
  margin-top: 8rpx;
}

.progress-track {
  height: 12rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 12rpx;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 6rpx;
  transition: width 1s ease-in-out;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
}

.progress-label {
  font-size: 22rpx;
  color: #87909c;
}

/* 分隔线 */
.divider {
  height: 2rpx;
  background: linear-gradient(90deg, transparent 0%, rgba(77, 142, 255, 0.2) 50%, transparent 100%);
  margin: 32rpx 0;
}

/* 评分说明 */
.score-desc {
  padding: 16rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 16rpx;
  margin-top: 16rpx;
}

.desc-text {
  font-size: 26rpx;
  color: #555e6d;
  line-height: 1.4;
}

/* 核心指标网格 */
.indicators-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx;
}

.indicator-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
  transition: all 0.3s ease;
  
  &:active {
    transform: translateY(-4rpx);
    box-shadow: 0 8rpx 32rpx rgba(45, 107, 255, 0.15);
  }
}

.indicator-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.indicator-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 12rpx;
}

.indicator-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #2d3b4e;
}

.indicator-value {
  display: flex;
  align-items: baseline;
  margin-bottom: 12rpx;
}

.indicator-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.status-text {
  font-size: 24rpx;
  font-weight: 500;
  
  &.status-normal {
    color: #10b981;
  }
  
  &.status-warning {
    color: #f59e0b;
  }
  
  &.status-danger {
    color: #ff6b6b;
  }
}

.trend-icon {
  width: 24rpx;
  height: 24rpx;
}

.trend-icon-img {
  width: 100%;
  height: 100%;
}

/* 图表占位符 */
.chart-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 16rpx;
  transition: all 0.3s ease;
}

.chart-placeholder:active {
  background: rgba(77, 142, 255, 0.1);
}

.chart-icon-mini {
  font-size: 24rpx;
  margin-right: 8rpx;
}

.chart-text {
  font-size: 24rpx;
  color: #4d8eff;
  font-weight: 500;
}

/* 健康趋势卡片 */
.trend-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.trend-header {
  display: flex;
  align-items: center;
  margin-bottom: 32rpx;
}

.trend-chart-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 12rpx;
}

.trend-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #2d3b4e;
}

/* 趋势图表 */
.trend-chart {
  display: flex;
  height: 300rpx;
  margin-bottom: 32rpx;
}

.y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 60rpx;
  padding-right: 20rpx;
}

.y-label {
  font-size: 22rpx;
  color: #87909c;
  text-align: right;
}

.chart-area {
  flex: 1;
  position: relative;
  border-left: 2rpx solid rgba(77, 142, 255, 0.2);
  border-bottom: 2rpx solid rgba(77, 142, 255, 0.2);
  padding-left: 20rpx;
}

.grid-lines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.grid-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1rpx;
  background: rgba(77, 142, 255, 0.1);
  
  &:nth-child(1) { top: 0; }
  &:nth-child(2) { top: 33.33%; }
  &:nth-child(3) { top: 66.66%; }
  &:nth-child(4) { top: 100%; }
}

.data-points {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.data-point {
  position: absolute;
  transform: translate(-50%, 50%);
}

.point-circle {
  width: 16rpx;
  height: 16rpx;
  background: #4d8eff;
  border: 3rpx solid #ffffff;
  border-radius: 50%;
  box-shadow: 0 4rpx 12rpx rgba(45, 107, 255, 0.3);
}

.point-value {
  position: absolute;
  top: -40rpx;
  left: 50%;
  transform: translateX(-50%);
  font-size: 20rpx;
  color: #4d8eff;
  font-weight: 600;
  white-space: nowrap;
}

.trend-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-bottom: 2rpx dashed rgba(77, 142, 255, 0.5);
}

.x-axis {
  display: flex;
  justify-content: space-between;
  margin-top: 20rpx;
  padding-left: 80rpx;
}

.x-label {
  font-size: 22rpx;
  color: #87909c;
  flex: 1;
  text-align: center;
}

/* 趋势分析 */
.trend-analysis {
  display: flex;
  justify-content: space-between;
  padding-top: 24rpx;
  border-top: 2rpx solid rgba(77, 142, 255, 0.1);
}

.analysis-item {
  display: flex;
  flex-direction: column;
}

.analysis-label {
  font-size: 24rpx;
  color: #87909c;
  margin-bottom: 8rpx;
}

.analysis-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #2d3b4e;
  
  &.trend-up {
    color: #10b981;
  }
  
  &.trend-down {
    color: #ff6b6b;
  }
  
  &.trend-stable {
    color: #f59e0b;
  }
}

/* 异常提醒 */
.alerts-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.alert-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border-left: 8rpx solid;
  transition: all 0.3s ease;
  
  &.warning {
    border-left-color: #f59e0b;
  }
  
  &.info {
    border-left-color: #4d8eff;
  }
  
  &.danger {
    border-left-color: #ff6b6b;
  }
  
  &:active {
    transform: translateY(-2rpx);
    box-shadow: 0 8rpx 32rpx rgba(45, 107, 255, 0.12);
  }
}

.alert-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.alert-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 16rpx;
}

.alert-info {
  flex: 1;
}

.alert-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
  display: block;
  margin-bottom: 4rpx;
}

.alert-time {
  font-size: 24rpx;
  color: #87909c;
}

.alert-more {
  width: 24rpx;
  height: 24rpx;
}

.alert-content {
  display: block;
  font-size: 28rpx;
  color: #555e6d;
  margin-bottom: 16rpx;
  line-height: 1.4;
}

.alert-suggestion {
  display: block;
  font-size: 26rpx;
  color: #4d8eff;
  font-weight: 500;
  line-height: 1.4;
}
</style>