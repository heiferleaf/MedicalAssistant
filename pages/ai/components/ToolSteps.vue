<template>
  <view v-if="steps && steps.length > 0" class="tool-steps-container">
    <view class="tool-step" v-for="(step, index) in steps" :key="index" :class="['step-' + step.status]">
      <!-- 状态图标 -->
      <view class="step-icon">
        <text v-if="step.status === 'success'">✅</text>
        <text v-else-if="step.status === 'processing'">🔄</text>
        <text v-else-if="step.status === 'error'">❌</text>
        <text v-else>⏳</text>
      </view>
      
      <!-- 步骤文字 -->
      <view class="step-content">
        <text class="step-description">{{ step.description }}</text>
        <text v-if="step.status === 'processing'" class="step-loading">执行中...</text>
        <text v-if="step.status === 'error'" class="step-error">{{ step.error }}</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'ToolSteps',
  props: {
    steps: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.tool-steps-container {
  margin: 8rpx 0;
  padding: 16rpx;
  background-color: #f8f9fa;
  border-radius: 12rpx;
}

.tool-step {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
}

.tool-step:not(:last-child) {
  border-bottom: 1rpx solid #e9ecef;
}

.step-icon {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  font-size: 32rpx;
}

.step-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.step-description {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
}

.step-loading {
  font-size: 24rpx;
  color: #007bff;
  margin-top: 4rpx;
}

.step-error {
  font-size: 24rpx;
  color: #dc3545;
  margin-top: 4rpx;
}

/* 不同状态的样式 */
.step-processing .step-description {
  color: #007bff;
}

.step-success .step-description {
  color: #28a745;
}

.step-error .step-description {
  color: #dc3545;
}
</style>
