<template>
  <view class="page-container">
		<view class="padding"></view>
    <view class="header">
      <view class="back-btn" @click="uni.navigateBack()">
        <image class="icon-sm" src="/static/Register/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">申请加入家庭组</text>
    </view>

    <scroll-view scroll-y class="main-content">
      <view class="content-wrapper">
        
        <view class="text-center pb-8">
          <view class="icon-wrapper">
            <image class="icon-lg" src="/static/family/group_add.svg" mode="aspectFit"></image>
          </view>
          <text class="main-title">申请加入家庭组</text>
          <text class="sub-title">输入您的家人的家庭组 ID，申请加入并共享健康数据。</text>
        </view>

        <view class="form-section">
          <view class="input-container">
            <text class="label">家庭组 ID (组号)</text>
            <view class="input-box">
              <input 
                class="custom-input" 
                type="text" 
                v-model="groupId" 
                maxlength="10" 
                placeholder="请输入家庭组编号" 
                placeholder-class="placeholder-text"
              />
            </view>
          </view>

          <button class="submit-btn" hover-class="submit-btn-hover" @click="submitApply">
            <text>提交入组申请</text>
            <image class="icon-md ml-2" src="/static/ai/send.svg" mode="aspectFit"></image>
          </button>

          <view class="info-card">
            <image class="icon-xs mt-1" src="/static/Mine/info.svg" mode="aspectFit"></image>
            <text class="info-text">注意：同一个组在 48 小时内仅能申请一次。</text>
          </view>
        </view>
      </view>
    </scroll-view>
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

<style lang="scss" scoped>
/* 颜色与基础变量 */
$primary: #4d88ff;
$bg-color: #ffffff;
$bg-light: #f5f6f8;
$text-main: #0f172a;
$text-sub: #64748b;
$border-color: #e2e8f0;

.padding {
  height: 64rpx; /* 顶部留白，适配状态栏 */
}

/* 图标通用尺寸 */
.icon-xs { width: 28rpx; height: 28rpx; flex-shrink: 0; }
.icon-sm { width: 40rpx; height: 40rpx; flex-shrink: 0; }
.icon-md { width: 48rpx; height: 48rpx; flex-shrink: 0; }
.icon-lg { width: 72rpx; height: 72rpx; flex-shrink: 0; }
.icon-tab { width: 48rpx; height: 48rpx; flex-shrink: 0; margin-bottom: 4rpx; }
.ml-2 { margin-left: 16rpx; }

.page-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: $bg-color;
  font-family: 'Inter', sans-serif;
}

/* 顶部导航 */
.header {
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  background-color: $bg-color;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  
  .back-btn {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    &:active { background-color: #f8fafc; }
  }
  
  .header-title {
    font-size: 36rpx;
    font-weight: bold;
    color: $text-main;
    margin-left: 16rpx;
  }
}

/* 主内容区 */
.main-content {
  flex: 1;
  height: 0;
  background-color: $bg-color;
  
  .content-wrapper {
    padding: 64rpx 48rpx 32rpx;
  }
}

/* 文本中心对齐与头部信息 */
.text-center { text-align: center; }
.pb-8 { padding-bottom: 64rpx; }

.icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 160rpx;
  height: 160rpx;
  background-color: rgba($primary, 0.1);
  border-radius: 50%;
  margin-bottom: 48rpx;
}

.main-title {
  font-size: 48rpx;
  font-weight: bold;
  color: $text-main;
  display: block;
  margin-bottom: 24rpx;
}

.sub-title {
  font-size: 32rpx;
  color: $text-sub;
  line-height: 1.6;
  display: block;
}

/* 表单区域 */
.form-section {
  display: flex;
  flex-direction: column;
  gap: 48rpx;
}

.input-container {
  .label {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: #334155;
    margin-bottom: 16rpx;
    margin-left: 8rpx;
  }
  
  .input-box {
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
    height: 112rpx;
    border: 2rpx solid $border-color;
    border-radius: 16rpx;
    background-color: #ffffff;
    box-sizing: border-box;
    transition: border-color 0.2s;
    
    /* 模拟 focus 状态 */
    &:focus-within {
      border-color: $primary;
    }
    
    .custom-input {
      flex: 1;
      height: 100%;
      padding: 0 96rpx 0 32rpx; /* 右侧留出扫码按钮的空间 */
      font-size: 32rpx;
      color: $text-main;
      background: transparent;
    }
    
    .placeholder-text {
      color: #94a3b8;
    }
    
    .scan-btn {
      position: absolute;
      right: 16rpx;
      top: 50%;
      transform: translateY(-50%);
      width: 72rpx;
      height: 72rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 12rpx;
      &:active { background-color: rgba($primary, 0.1); }
    }
  }
}

/* 按钮 */
.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 112rpx;
  background-color: $primary;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba($primary, 0.2);
  border: none;
  
  &::after { border: none; }
}

.submit-btn-hover {
  background-color: #3b76eb;
  transform: scale(0.99);
}

/* 提示卡片 */
.info-card {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  background-color: #f8fafc;
  padding: 32rpx;
  border-radius: 24rpx;
  border: 2rpx solid #f1f5f9;
  
  .info-text {
    flex: 1;
    font-size: 24rpx;
    color: #64748b;
    line-height: 1.5;
  }
}

/* 底部导航栏 */
.custom-tab-bar {
  display: flex;
  border-top: 1px solid #f1f5f9;
  background-color: $bg-color;
  padding: 16rpx 32rpx 32rpx;
  
  &.pb-safe {
    padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
  }
  
  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    
    .tab-text {
      font-size: 20rpx;
      font-weight: 500;
      color: #94a3b8;
    }
    
    &.active {
      .tab-text { color: $primary; }
      /* 此处如果想要 active 状态的图标变色，建议准备两套 svg (如 group.svg 和 group_active.svg) */
    }
  }
}
</style>