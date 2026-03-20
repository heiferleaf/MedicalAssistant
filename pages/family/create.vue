<template>
  <view class="page-container">
    <view class="nav-bar">
      <view class="back-btn" @click="uni.navigateBack()">
        <image class="back-icon" src="/static/Register/back.png" mode="aspectFit" />
      </view>
      <text class="title">创建家庭组</text>
      <view class="placeholder"></view>
    </view>

    <scroll-view scroll-y class="content-scroll">
      <view class="header-section">
        <view class="icon-wrapper">
          <image class="icon-group" src="/static/family/group_add.svg" mode="aspectFit" />
        </view>
        <text class="main-title">创建家庭组</text>
        <text class="sub-title">建立一个私密的健康管理空间，邀请家人共同关注用药情况。</text>
      </view>

      <view class="form-section">
        <view class="form-item">
          <text class="label">家庭组名称</text>
          <input class="custom-input" type="text" v-model="formData.groupName" placeholder="例如：幸福一家人"
            placeholder-class="placeholder-text" />
        </view>

        <view class="form-item">
          <text class="label">描述 (可选)</text>
          <textarea class="custom-textarea" v-model="formData.description" placeholder="输入家庭组的简单描述，例如：全家人的健康小助手..."
            placeholder-class="placeholder-text" />
        </view>

        <view class="info-card">
          <text class="material-symbols-outlined icon-info">info</text>
          <text class="info-text">创建后您将自动成为该组组长，拥有管理家庭成员和用药提醒的完整权限。</text>
        </view>
      </view>
    </scroll-view>

    <view class="footer-action pb-safe">
      <button class="submit-btn" hover-class="submit-btn-hover" @click="handleCreate">
        立即创建并成为组长
      </button>
      <text class="agreement-text">点击创建即代表您同意《家庭组管理协议》</text>
    </view>
  </view>
</template>

<script>
import familyApi from '../../api/family';
export default {
  data() {
    return {
      formData: {
        groupName: '', // 对应 groupName 字段 
        description: '' // 对应 description 字段 
      },
      submitting: false
    };
  },
  methods: {
    async handleCreate() {
      if (!this.formData.groupName) {
        uni.showToast({ title: '请输入家庭组名称', icon: 'none' });
        return;
      }

      this.submitting = true;
      try {
        // 调用创建接口，后台会校验用户是否已在其他组 
        // 后台逻辑：创建成功自动将用户ID赋给 owner_user_id [cite: 11, 41]
        const res = await familyApi.createGroup(this.formData);

        if (res.code === 200) {
          uni.showToast({ title: '创建成功', icon: 'success' });

          // 创建成功后自动变成组长，并跳转至成员页面 [cite: 11, 15]
          setTimeout(() => {
            uni.reLaunch({
              url: '/pages/group/members'
            });
          }, 1500);
        } else {
          uni.showToast({ title: res.message || '创建失败', icon: 'none' });
        }
      } catch (e) {
        console.error("报错显示：", e)
        uni.showToast({ title: '网络请求异常', icon: 'none' });
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>



<style lang="scss" scoped>
/* 颜色变量 */
$primary: #4d88ff;
$bg-color: #ffffff;
$text-main: #0f172a;
$text-sub: #64748b;
$border-color: #e2e8f0;

.page-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: $bg-color;
  font-family: 'Inter', sans-serif;
}

/* 顶部导航栏 */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  background-color: $bg-color;
  position: sticky;
  top: 0;
  z-index: 10;

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

  .icon-back {
    font-size: 48rpx;
    color: $text-main;
  }

  .title {
    font-size: 36rpx;
    font-weight: bold;
    color: $text-main;
    flex: 1;
    text-align: center;
  }

  .placeholder {
    width: 80rpx;
  }
}

/* 滚动内容区 */
.content-scroll {
  flex: 1;
  height: 0;
  /* 重要：让 scroll-view 撑满剩余空间 */
}

/* 头部区域 */
.header-section {
  padding: 40rpx 48rpx 16rpx;

  .icon-wrapper {
    width: 128rpx;
    height: 128rpx;
    background-color: rgba($primary, 0.1);
    border-radius: 32rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 48rpx;

    .icon-group {
      width: 80rpx;
      height: 80rpx;
    }
  }

  .main-title {
    font-size: 48rpx;
    font-weight: bold;
    color: $text-main;
    display: block;
    margin-bottom: 16rpx;
  }

  .sub-title {
    font-size: 32rpx;
    color: $text-sub;
    line-height: 1.6;
    display: block;
  }
}

/* 表单区域 */
.form-section {
  padding: 48rpx;

  .form-item {
    margin-bottom: 48rpx;

    .label {
      font-size: 28rpx;
      font-weight: 600;
      color: #1e293b;
      margin-bottom: 16rpx;
      display: block;
    }

    .custom-input,
    .custom-textarea {
      width: 100%;
      box-sizing: border-box;
      background-color: #f8fafc;
      border: 2rpx solid $border-color;
      border-radius: 16rpx;
      padding: 0 32rpx;
      font-size: 32rpx;
      color: $text-main;
      transition: all 0.3s;
    }

    .custom-input {
      height: 112rpx;
    }

    .custom-textarea {
      height: 240rpx;
      padding: 32rpx;
    }

    .placeholder-text {
      color: #94a3b8;
    }
  }
}

/* 提示卡片 */
.info-card {
  background-color: rgba($primary, 0.05);
  border: 2rpx solid rgba($primary, 0.1);
  border-radius: 24rpx;
  padding: 32rpx;
  display: flex;
  align-items: flex-start;

  .icon-info {
    color: $primary;
    font-size: 40rpx;
    margin-right: 16rpx;
    margin-top: 4rpx;
  }

  .info-text {
    font-size: 28rpx;
    color: rgba($primary, 0.8);
    line-height: 1.6;
    flex: 1;
  }
}

/* 底部操作区 */
.footer-action {
  padding: 48rpx;
  background-color: $bg-color;
  border-top: 2rpx solid #f1f5f9;

  /* 适配 iPhone 底部安全区 */
  &.pb-safe {
    padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
  }

  .submit-btn {
    background-color: $primary;
    color: #ffffff;
    font-size: 36rpx;
    font-weight: bold;
    height: 112rpx;
    line-height: 112rpx;
    border-radius: 24rpx;
    box-shadow: 0 8rpx 24rpx rgba($primary, 0.2);
    border: none;

    &::after {
      border: none;
    }
  }

  .submit-btn-hover {
    background-color: #3b76eb;
    transform: scale(0.98);
  }

  .agreement-text {
    display: block;
    text-align: center;
    font-size: 24rpx;
    color: #94a3b8;
    margin-top: 32rpx;
  }
}
</style>