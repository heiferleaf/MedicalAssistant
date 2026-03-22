<template>
  <view class="page-container">
		<view class="padding"></view>
    <view class="header">
      <view class="back-btn" @click="uni.navigateBack()">
        <image class="icon-sm" src="/static/Register/back.png" mode="aspectFit"></image>
      </view>
      <text class="header-title">添加家庭成员</text>
      <view class="placeholder"></view> </view>

    <scroll-view scroll-y class="main-content">
      <view class="content-wrapper">
        
        <view class="hero-section">
          <view class="icon-wrapper">
            <image class="icon-lg" src="/static/family/person_add_blue.svg" mode="aspectFit"></image>
          </view>
          <text class="hero-title">邀请成员</text>
          <text class="hero-desc">与您的家人联系，共同管理健康并分享设备访问权限。</text>
        </view>

        <view class="form-section">
          <view class="form-group">
            <text class="label">手机号码</text>
            <view class="input-wrapper">
              <image class="input-icon" src="/static/family/call.svg" mode="aspectFit"></image>
              <input 
                class="custom-input" 
                type="number" 
                maxlength="11"
                v-model="phoneNumber"
                placeholder="请输入手机号码" 
                placeholder-class="placeholder-text"
              />
            </view>
          </view>

          <view class="form-group">
            <text class="label">备注信息</text>
            <view class="input-wrapper">
              <image class="textarea-icon" src="/static/DrugScan/note.svg" mode="aspectFit"></image>
              <textarea 
                class="custom-textarea" 
                v-model="remark"
                placeholder="例如：配偶、孩子、长辈" 
                placeholder-class="placeholder-text"
              ></textarea>
            </view>
          </view>

          <view class="btn-container">
            <button class="submit-btn" hover-class="submit-btn-hover" @click="handleInvite">
              <text>发送邀请</text>
              <image class="icon-md ml-2" src="/static/ai/send.svg" mode="aspectFit"></image>
            </button>
          </view>
        </view>

        <view class="info-box">
          <image class="icon-sm shrink-0 mt-1" src="/static/Mine/info.svg" mode="aspectFit"></image>
          <text class="info-text">邀请将通过短信发送。您的家人将收到下载应用的链接，并自动加入您的家庭组。</text>
        </view>
        
      </view>
    </scroll-view>

    <view class="footer pb-safe">
      <text class="footer-text">由 FAMILYSYNC 提供安全保障</text>
    </view>
  </view>
</template>

<script>
import familyApi from '../../api/family';
export default {
  data() {
    return {
      groupId: "",
      phoneNumber: "",
      remark: "", // 对应你说的备注字段
    };
  },
  onLoad(options) {
    if (options.groupId) {
      this.groupId = options.groupId;
    }
  },
  methods: {
    async handleInvite() {
      // 1. 基础校验
      if (!this.phoneNumber) {
        return uni.showToast({ title: '请输入手机号', icon: 'none' });
      }
      if (!/^1[3-9]\d{9}$/.test(this.phoneNumber)) {
        return uni.showToast({ title: '手机号格式不正确', icon: 'none' });
      }

      uni.showLoading({ title: '发送中...' });

      try {
        // 2. 调用接口：传递 groupId, phoneNumber 和 data(含mark)
        let data = {
          remark: this.remark || "邀请你加入我的家庭组~"
        }
        const res = await familyApi.inviteMember(this.groupId, this.phoneNumber, data
        );

        console.log("res: ", res);

        if (res.code === 200) {
          uni.showToast({ title: '邀请已发送', icon: 'success' });
          // 成功后延迟返回上一页
          setTimeout(() => uni.navigateBack(), 1500);
        } else {
          // uni.showToast({ title: res.message || '发送失败', icon: 'none' });
        }
      } catch (e) {
        console.log("报错显示:", e);
        // uni.showToast({ title: '服务器繁忙', icon: 'none' });
      } finally {
        uni.hideLoading();
      }
    }
  }
};
</script>

<style lang="scss" scoped>
/* 全局变量 */
$primary: #4d88ff;
$bg-color: #f5f6f8;
$card-bg: #ffffff;
$text-main: #0f172a;
$text-sub: #64748b;
$border-color: #e2e8f0;

.padding {
  height: 64rpx; /* 顶部留白，适配状态栏 */
}

/* 图标基础尺寸 */
.icon-sm { width: 40rpx; height: 40rpx; flex-shrink: 0; }
.icon-md { width: 48rpx; height: 48rpx; flex-shrink: 0; }
.icon-lg { width: 64rpx; height: 64rpx; flex-shrink: 0; }
.ml-2 { margin-left: 16rpx; }
.shrink-0 { flex-shrink: 0; }
.mt-1 { margin-top: 4rpx; }

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
  justify-content: space-between;
  padding: 20rpx 32rpx;
  background-color: rgba(245, 246, 248, 0.9);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 10;
  
  .back-btn {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    &:active { background-color: rgba($primary, 0.1); }
  }
  
  .header-title {
    font-size: 36rpx;
    font-weight: bold;
    color: $text-main;
  }
  
  .placeholder { width: 80rpx; }
}

/* 主内容区 */
.main-content {
  flex: 1;
  height: 0;
  
  .content-wrapper {
    padding: 64rpx 48rpx 96rpx;
    display: flex;
    flex-direction: column;
  }
}

/* 头部说明 */
.hero-section {
  margin-bottom: 64rpx;
  
  .icon-wrapper {
    width: 128rpx;
    height: 128rpx;
    background-color: rgba($primary, 0.1);
    border-radius: 32rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 48rpx;
  }
  
  .hero-title {
    font-size: 48rpx;
    font-weight: bold;
    color: $text-main;
    display: block;
    margin-bottom: 16rpx;
  }
  
  .hero-desc {
    font-size: 30rpx;
    color: $text-sub;
    line-height: 1.5;
    display: block;
  }
}

/* 表单区域 */
.form-section {
  display: flex;
  flex-direction: column;
  gap: 48rpx;
  margin-bottom: 80rpx;
  
  .form-group {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
    
    .label {
      font-size: 28rpx;
      font-weight: 600;
      color: #334155;
      margin-left: 8rpx;
    }
    
    .input-wrapper {
      position: relative;
      width: 100%;
      
      .input-icon {
        position: absolute;
        left: 32rpx;
        top: 50%;
        transform: translateY(-50%);
        width: 44rpx;
        height: 44rpx;
        opacity: 0.5;
        z-index: 1;
      }
      
      .textarea-icon {
        position: absolute;
        left: 32rpx;
        top: 32rpx;
        width: 44rpx;
        height: 44rpx;
        opacity: 0.5;
        z-index: 1;
      }
      
      .custom-input, .custom-textarea {
        width: 100%;
        box-sizing: border-box;
        background-color: $card-bg;
        border: 2rpx solid $border-color;
        border-radius: 24rpx;
        color: $text-main;
        font-size: 32rpx;
        transition: border-color 0.2s, box-shadow 0.2s;
        
        &:focus {
          border-color: $primary;
          box-shadow: 0 0 0 4rpx rgba($primary, 0.1);
        }
      }
      
      .custom-input {
        height: 112rpx;
        padding: 0 32rpx 0 96rpx; /* 左侧留出图标空间 */
      }
      
      .custom-textarea {
        height: 240rpx;
        padding: 32rpx 32rpx 32rpx 96rpx; /* 左侧留出图标空间 */
      }
      
      .placeholder-text { color: #94a3b8; }
    }
  }
  
  .btn-container {
    padding-top: 16rpx;
    
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
    .submit-btn-hover { background-color: #3b76eb; transform: scale(0.99); }
  }
}

/* 底部提示盒子 */
.info-box {
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
  padding: 32rpx;
  background-color: rgba($primary, 0.05);
  border: 2rpx solid rgba($primary, 0.1);
  border-radius: 24rpx;
  
  .info-text {
    font-size: 26rpx;
    line-height: 1.6;
    color: #475569;
  }
}

/* 底部版权说明 */
.footer {
  text-align: center;
  padding: 48rpx;
  
  &.pb-safe {
    padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
  }
  
  .footer-text {
    font-size: 20rpx;
    font-weight: 500;
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 4rpx;
  }
}
</style>