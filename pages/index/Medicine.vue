<template>
  <view class="page-container">
    <view class="status-bar"></view>

    <view class="header">
      <view class="header-left">
        <text class="page-title">健康数据</text>
        <text class="page-subtitle">实时监控您的身体状态</text>
      </view>
      <view class="header-right">
        <view class="report-btn" @click="generateReport">
          <text class="report-text">分析报告</text>
          <text class="report-icon">↗</text>
        </view>
      </view>
    </view>

    <scroll-view 
      class="main-content"
      scroll-y
      :show-scrollbar="false"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="section">
        <view class="overview-card">
          <view class="overview-top">
            <view class="overview-main">
              <text class="overview-label">服药依从性</text>
              <view class="overview-value-box">
                <text class="value-number">{{ adherenceRate }}</text>
                <text class="value-unit">%</text>
              </view>
            </view>
            <view class="trend-tag up">
              <text class="trend-arrow">▲</text>
              <text>较上周+5%</text>
            </view>
          </view>
          
          <view class="progress-container">
            <view class="progress-track">
              <view class="progress-fill" :style="{ width: adherenceRate + '%' }"></view>
            </view>
            <view class="progress-info">
              <text>坚持就是胜利，本周表现优于 90% 用户</text>
            </view>
          </view>

          <view class="divider"></view>

          <view class="overview-bottom">
            <view class="score-info">
              <text class="overview-label">健康综合评分</text>
              <text class="score-desc">{{ getScoreDescription(healthScore) }}</text>
            </view>
            <view class="score-circle" :class="healthScoreLevel">
              <text class="score-num">{{ healthScore }}</text>
              <text class="score-text">{{ getHealthScoreText(healthScore) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-header">
          <text class="section-title">核心体征</text>
          <view class="time-picker-wrap">
            <picker :range="timeRanges" @change="onTimeRangeChange">
              <view class="time-picker">
                <text>{{ selectedTimeRange }}数据</text>
                <text class="icon-down">▼</text>
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
            <view class="card-header">
              <view class="icon-bg" :class="indicator.statusClass">
                <image class="card-icon" :src="indicator.icon" mode="aspectFit" />
              </view>
              <text class="status-dot" :class="indicator.statusClass"></text>
            </view>
            <view class="card-body">
              <text class="card-name">{{ indicator.name }}</text>
              <view class="card-value-row">
                <text class="card-value">{{ indicator.value }}</text>
                <text class="card-unit">{{ indicator.unit }}</text>
              </view>
            </view>
            <view class="card-footer">
              <text class="card-status" :class="indicator.statusClass">{{ indicator.status }}</text>
              <text class="card-trend" :class="indicator.trend">{{ indicator.trend === 'up' ? '↗' : indicator.trend === 'down' ? '↘' : '→' }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-header">
          <text class="section-title">指标趋势</text>
          <scroll-view scroll-x class="tab-scroll" :show-scrollbar="false">
            <view 
              v-for="(opt, index) in trendOptions" 
              :key="index"
              class="tab-item"
              :class="{active: trendIndex === index}"
              @click="trendIndex = index"
            >
              {{ opt }}
            </view>
          </scroll-view>
        </view>
        
        <view class="trend-card">
          <view class="chart-header">
            <view class="chart-legend">
              <view class="legend-dot"></view>
              <text>{{ selectedTrend }} ({{ indicatorUnitMap[selectedTrend] }})</text>
            </view>
            <text class="avg-label">周期均值: {{ averageValue }}</text>
          </view>
          
          <view class="mock-chart">
            <view class="y-axis">
              <text v-for="label in ['max', 'mid', 'min']" :key="label">{{ label }}</text>
            </view>
            <view class="chart-content">
              <view class="grid-line" v-for="i in 3" :key="i"></view>
              <view class="data-line">
                <view 
                  class="data-node" 
                  v-for="(point, pIdx) in trendData" 
                  :key="pIdx"
                  :style="{ left: point.x + '%', bottom: point.y + '%' }"
                >
                  <view class="node-dot"></view>
                  <view class="node-tip">{{ point.value }}</view>
                </view>
              </view>
            </view>
            <view class="x-axis">
              <text v-for="day in xAxisLabels" :key="day">{{ day }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section" v-if="alerts.length > 0">
        <view class="section-header">
          <text class="section-title">异常预警</text>
          <text class="alert-count">{{ alerts.length }} 条待处理</text>
        </view>
        
        <view class="alert-stack">
          <view 
            class="alert-item"
            v-for="alert in alerts"
            :key="alert.id"
            :class="alert.level"
            @click="viewAlertDetail(alert.id)"
          >
            <view class="alert-left">
              <view class="alert-dot"></view>
            </view>
            <view class="alert-mid">
              <view class="alert-title-row">
                <text class="alert-title">{{ alert.title }}</text>
                <text class="alert-time">{{ alert.time }}</text>
              </view>
              <text class="alert-msg">{{ alert.content }}</text>
            </view>
            <text class="alert-arrow">〉</text>
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
      refreshing: false,
      adherenceRate: 92,
      healthScore: 88,
      timeIndex: 0,
      trendIndex: 0,
      timeRanges: ['最近7天', '最近30天', '最近90天'],
      trendOptions: ['血压', '心率', '血糖', '体重'],
      indicatorUnitMap: { '血压': 'mmHg', '心率': 'bpm', '血糖': 'mmol/L', '体重': 'kg' },
      indicators: [
        { id: 1, name: '血压', value: '120/80', unit: 'mmHg', status: '正常', statusClass: 'status-normal', trend: 'stable', icon: '/static/Prepare/blood pressure.svg' },
        { id: 2, name: '心率', value: '72', unit: 'bpm', status: '正常', statusClass: 'status-normal', trend: 'up', icon: '/static/Home/heart.svg' },
        { id: 3, name: '血糖', value: '5.2', unit: 'mmol/L', status: '偏高', statusClass: 'status-warning', trend: 'down', icon: '/static/Prepare/blood-sugar.svg' },
        { id: 4, name: '体重', value: '65', unit: 'kg', status: '正常', statusClass: 'status-normal', trend: 'down', icon: '/static/Prepare/weight.svg' }
      ],
      trendData: [
        { x: 5, y: 30, value: '115' },
        { x: 30, y: 55, value: '128' },
        { x: 55, y: 45, value: '122' },
        { x: 80, y: 70, value: '135' },
        { x: 95, y: 50, value: '125' }
      ],
      xAxisLabels: ['周一', '周二', '周三', '周四', '今日'],
      averageValue: '126 mmHg',
      alerts: [
        { id: 1, level: 'danger', title: '血压异常升高', time: '14:30', content: '测量结果 145/95 mmHg，已超过警戒线。' },
        { id: 2, level: 'warning', title: '指标缺失', time: '09:00', content: '您今天尚未记录空腹血糖数据。' }
      ]
    }
  },
  computed: {
    selectedTimeRange() { return this.timeRanges[this.timeIndex] },
    selectedTrend() { return this.trendOptions[this.trendIndex] },
    healthScoreLevel() {
      if (this.healthScore >= 90) return 'level-excellent'
      if (this.healthScore >= 80) return 'level-good'
      return 'level-warn'
    }
  },
  methods: {
    onRefresh() {
      this.refreshing = true;
      setTimeout(() => { this.refreshing = false }, 1500);
    },
    onTimeRangeChange(e) { this.timeIndex = e.detail.value },
    getHealthScoreText(score) {
      return score >= 90 ? '优秀' : score >= 80 ? '良好' : '注意'
    },
    getScoreDescription(score) {
      return score >= 80 ? '您的健康状况非常稳定，请继续保持。' : '近期有指标波动，建议关注预警。'
    },
    generateReport() {
      uni.showLoading({ title: '分析中...' });
      setTimeout(() => uni.hideLoading(), 2000);
    },
    viewIndicatorDetail(id) {
      console.log('查看指标详情', id)
    }
  }
}
</script>

<style lang="scss" scoped>
// 变量定义
$primary-color: #4d8eff;
$success-color: #10b981;
$warning-color: #f59e0b;
$danger-color: #ff4d4f;
$bg-color: #f7f9fc;
$text-main: #2d3b4e;
$text-light: #87909c;

.page-container {
  min-height: 100vh;
  background-color: $bg-color;
  color: $text-main;
}

.status-bar { height: var(--status-bar-height); background-color: #fff; }

// 头部设计
.header {
  padding: 40rpx 32rpx;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 0 0 40rpx 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.05);

  .page-title {
    font-size: 44rpx;
    font-weight: 800;
    display: block;
    color: $text-main;
  }
  .page-subtitle {
    font-size: 24rpx;
    color: $text-light;
    margin-top: 8rpx;
  }
  
  .report-btn {
    background: rgba($primary-color, 0.1);
    padding: 16rpx 28rpx;
    border-radius: 20rpx;
    display: flex;
    align-items: center;
    
    .report-text {
      color: $primary-color;
      font-size: 26rpx;
      font-weight: 600;
    }
    .report-icon {
      color: $primary-color;
      margin-left: 8rpx;
      font-size: 24rpx;
    }
  }
}

.main-content {
  height: calc(100vh - 280rpx);
  padding: 32rpx;
  box-sizing: border-box;
}

.section {
  margin-bottom: 40rpx;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    padding: 0 8rpx;
    
    .section-title {
      font-size: 32rpx;
      font-weight: 700;
    }
    
    .time-picker {
      font-size: 24rpx;
      color: $primary-color;
      background: #fff;
      padding: 8rpx 20rpx;
      border-radius: 30rpx;
      display: flex;
      align-items: center;
      .icon-down { font-size: 16rpx; margin-left: 8rpx; }
    }
  }
}

// 概览卡片
.overview-card {
  background: #fff;
  border-radius: 32rpx;
  padding: 40rpx;
  box-shadow: 0 10rpx 30rpx rgba($primary-color, 0.08);

  .overview-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    
    .overview-label { font-size: 28rpx; color: $text-light; }
    .overview-value-box {
      margin-top: 10rpx;
      .value-number { font-size: 72rpx; font-weight: 800; color: $primary-color; }
      .value-unit { font-size: 28rpx; color: $text-light; margin-left: 8rpx; }
    }
    
    .trend-tag {
      padding: 8rpx 16rpx;
      border-radius: 12rpx;
      font-size: 22rpx;
      font-weight: 600;
      &.up { background: rgba($success-color, 0.1); color: $success-color; }
      .trend-arrow { margin-right: 4rpx; }
    }
  }

  .progress-container {
    margin-top: 30rpx;
    .progress-track {
      height: 16rpx;
      background: #f0f4f8;
      border-radius: 8rpx;
      overflow: hidden;
      .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, $primary-color, #83b2ff);
        border-radius: 8rpx;
      }
    }
    .progress-info {
      margin-top: 16rpx;
      font-size: 22rpx;
      color: $text-light;
    }
  }

  .divider { height: 2rpx; background: #f0f3f6; margin: 40rpx 0; }

  .overview-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .score-info {
      flex: 1;
      .score-desc { display: block; font-size: 24rpx; color: $text-light; margin-top: 8rpx; width: 80%; }
    }
    
    .score-circle {
      width: 110rpx;
      height: 110rpx;
      border-radius: 50%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      border: 6rpx solid;
      
      &.level-excellent { border-color: $success-color; color: $success-color; }
      &.level-good { border-color: $primary-color; color: $primary-color; }
      
      .score-num { font-size: 36rpx; font-weight: 800; line-height: 1; }
      .score-text { font-size: 20rpx; font-weight: 600; margin-top: 4rpx; }
    }
  }
}

// 指标网格
.indicators-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
  
  .indicator-card {
    background: #fff;
    border-radius: 28rpx;
    padding: 30rpx;
    position: relative;
    transition: all 0.2s;
    &:active { transform: scale(0.97); background: #fafbfc; }
    
    .card-header {
      display: flex;
      justify-content: space-between;
      .icon-bg {
        width: 64rpx;
        height: 64rpx;
        border-radius: 16rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        &.status-normal { background: rgba($primary-color, 0.08); }
        &.status-warning { background: rgba($warning-color, 0.08); }
        .card-icon { width: 36rpx; height: 36rpx; }
      }
      .status-dot {
        width: 12rpx;
        height: 12rpx;
        border-radius: 50%;
        &.status-normal { background: $success-color; }
        &.status-warning { background: $warning-color; }
      }
    }
    
    .card-body {
      margin-top: 24rpx;
      .card-name { font-size: 26rpx; color: $text-light; }
      .card-value-row {
        margin-top: 8rpx;
        .card-value { font-size: 36rpx; font-weight: 700; color: $text-main; }
        .card-unit { font-size: 22rpx; color: $text-light; margin-left: 6rpx; }
      }
    }
    
    .card-footer {
      margin-top: 20rpx;
      display: flex;
      justify-content: space-between;
      align-items: center;
      .card-status { font-size: 22rpx; font-weight: 600; }
      .card-trend {
        font-size: 24rpx;
        &.up { color: $danger-color; }
        &.down { color: $success-color; }
        &.stable { color: $text-light; }
      }
    }
  }
}

// 图表选项卡
.tab-scroll {
  width: 60%;
  white-space: nowrap;
  .tab-item {
    display: inline-block;
    padding: 8rpx 24rpx;
    font-size: 24rpx;
    color: $text-light;
    border-radius: 20rpx;
    &.active { color: $primary-color; background: rgba($primary-color, 0.1); font-weight: 600; }
  }
}

// 趋势图
.trend-card {
  background: #fff;
  border-radius: 32rpx;
  padding: 32rpx;
  
  .chart-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 40rpx;
    .chart-legend {
      display: flex;
      align-items: center;
      font-size: 24rpx;
      font-weight: 600;
      .legend-dot { width: 12rpx; height: 12rpx; background: $primary-color; border-radius: 50%; margin-right: 12rpx; }
    }
    .avg-label { font-size: 22rpx; color: $text-light; }
  }
  
  .mock-chart {
    height: 300rpx;
    display: flex;
    position: relative;
    
    .y-axis {
      width: 60rpx;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      font-size: 18rpx;
      color: #ccc;
      padding-bottom: 40rpx;
    }
    
    .chart-content {
      flex: 1;
      position: relative;
      border-left: 2rpx solid #f0f0f0;
      border-bottom: 2rpx solid #f0f0f0;
      margin-bottom: 40rpx;
      
      .grid-line {
        position: absolute; width: 100%; height: 2rpx; background: #fafafa;
        &:nth-child(1) { top: 0; }
        &:nth-child(2) { top: 50%; }
        &:nth-child(3) { top: 100%; }
      }
      
      .data-node {
        position: absolute;
        .node-dot {
          width: 14rpx; height: 14rpx; background: $primary-color; 
          border: 4rpx solid #fff; border-radius: 50%;
          box-shadow: 0 4rpx 10rpx rgba($primary-color, 0.3);
        }
        .node-tip {
          position: absolute; top: -34rpx; left: 50%; transform: translateX(-50%);
          font-size: 18rpx; font-weight: 700; color: $primary-color;
        }
      }
    }
    
    .x-axis {
      position: absolute; bottom: 0; left: 60rpx; right: 0;
      display: flex; justify-content: space-between;
      font-size: 20rpx; color: $text-light;
    }
  }
}

// 预警堆栈
.alert-stack {
  .alert-item {
    background: #fff;
    border-radius: 24rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
    display: flex;
    align-items: center;
    border-left: 10rpx solid transparent;
    
    &.danger { border-left-color: $danger-color; background: rgba($danger-color, 0.02); }
    &.warning { border-left-color: $warning-color; background: rgba($warning-color, 0.02); }
    
    .alert-left {
      margin-right: 20rpx;
      .alert-dot { width: 16rpx; height: 16rpx; border-radius: 50%; background: currentColor; }
    }
    
    .alert-mid {
      flex: 1;
      .alert-title-row {
        display: flex; justify-content: space-between; align-items: center;
        .alert-title { font-size: 28rpx; font-weight: 700; }
        .alert-time { font-size: 22rpx; color: $text-light; }
      }
      .alert-msg { font-size: 24rpx; color: #666; margin-top: 6rpx; display: block; }
    }
    
    .alert-arrow { font-size: 24rpx; color: #ccc; margin-left: 10rpx; }
  }
}

.alert-count { font-size: 22rpx; color: $danger-color; background: rgba($danger-color, 0.1); padding: 4rpx 16rpx; border-radius: 20rpx; }
</style>