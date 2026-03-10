<template>
  <view class="join-container">
    <view class="header-section">
      <text class="main-title">申请加入家庭组</text>
      <text class="tips">输入您家人的家庭组 ID，申请加入并共享健康数据。</text>
    </view>

    <view class="search-card">
      <view class="input-group">
        <text class="label">家庭组 ID (组号)</text>
        <view class="input-wrapper">
          <input type="number" class="id-input" v-model="groupId" placeholder="请输入 10 位家庭组编号" maxlength="10" />
          <text class="scan-icon" @click="handleScan">扫码</text>
        </view>
      </view>
    </view>

    <view class="action-section">
      <button class="join-btn" :loading="submitting" :disabled="!groupId" @click="submitApply">
        提交入组申请
      </button>
      <view class="notice-box">
        <text class="notice-text">注意：同一个组在 48 小时内仅能申请一次。</text>
      </view>
    </view>
  </view>
</template>

<script>
import familyApi from '../../api/family';
export default {
  data() {
    return {
      groupId: '', // 对应接口中的 group_id
      remark: '',  // 对应流水表中的 remark 字段
      submitting: false
    };
  },
  methods: {
    // 处理扫码（需求文档提及支持二维码方式）
    handleScan() {
      uni.scanCode({
        success: (res) => {
          // 假设二维码内容就是组号
          this.groupId = res.result;
        }
      });
    },

    async submitApply() {
      if (!this.groupId) {
        uni.showToast({ title: '请输入组号', icon: 'none' });
        return;
      }

      this.submitting = true;
      try {
        // 调用申请加入接口 (UC2)
        // 后台需校验：用户未进组、该组有空位、48小时内无重复申请
        const res = await familyApi.applyJoinGroup(this.groupId);

        if (res.code === 200) {
          uni.showModal({
            title: '申请已提交',
            content: '您的申请已发送给组长，请等待审核。',
            showCancel: false,
            success: () => {
              // 提交成功后，跳转到“我的消息”页面查看处理进度
              uni.navigateBack();
            }
          });
        } else {
          // 处理业务限制（如：48小时重复申请、组已满等）
          uni.showToast({
            title: res.message || '申请失败',
            icon: 'none',
            duration: 2000
          });
        }
      } catch (e) {
        console.error(e);
        uni.showToast({ title: '网络繁忙', icon: 'none' });
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.join-container {
  padding: 60rpx 40rpx;
  background-color: #f8f9fb;
  min-height: 100vh;
}

.header-section {
  margin-bottom: 50rpx;
}

.main-title {
  font-size: 48rpx;
  font-weight: bold;
  color: #2c3e50;
  display: block;
}

.tips {
  font-size: 26rpx;
  color: #7f8c8d;
  margin-top: 16rpx;
  line-height: 1.6;
}

.search-card {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.label {
  font-size: 28rpx;
  color: #34495e;
  font-weight: 600;
  margin-bottom: 20rpx;
  display: block;
}

.input-wrapper {
  display: flex;
  align-items: center;
  border-bottom: 2rpx solid #ebedf0;
  padding-bottom: 10rpx;
}

.id-input {
  flex: 1;
  height: 80rpx;
  font-size: 40rpx;
  color: #2c3e50;
  letter-spacing: 2rpx;
}

.scan-icon {
  font-size: 26rpx;
  color: #007AFF;
  padding: 10rpx 20rpx;
  border-left: 1px solid #eee;
}

.remark-box {
  width: 100%;
  height: 160rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.action-section {
  margin-top: 60rpx;
}

.join-btn {
  background-color: #007AFF;
  color: white;
  border-radius: 50rpx;
  height: 100rpx;
  line-height: 100rpx;
  font-size: 32rpx;
}

.notice-box {
  margin-top: 30rpx;
  text-align: center;
}

.notice-text {
  font-size: 24rpx;
  color: #95a5a6;
}
</style>