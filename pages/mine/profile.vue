<template>
  <view class="profile-page">
    <view class="padding"></view>
    <!-- 背景装饰与渐变 -->
    <view class="bg-mesh"></view>
    <view class="blob blob-1"></view>
    <view class="blob blob-2"></view>
    
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="back-btn" @tap="goBack">
        <text class="material-symbols-outlined">arrow_back</text>
      </view>
      <text class="nav-title">个人资料管理</text>
      <view class="spacer"></view>
    </view>

    <scroll-view scroll-y class="scroll-container" :show-scrollbar="false">
      <!-- 英雄区：整体调小 -->
      <view class="hero-section">
        <text class="hero-title">完善您的<text class="primary">\n健康档案</text></text>
        <text class="hero-desc">更新资料，以便为您提供更精准的建议。</text>
      </view>

      <!-- 信息修改卡片 -->
      <view class="form-card">
        <!-- 账号：只读 -->
        <view class="input-group">
          <text class="field-label">账号 (不可更改)</text>
          <view class="input-wrapper disabled-box">
            <text class="material-symbols-outlined field-icon">account_circle</text>
            <input v-model="form.username" disabled class="input-box" />
          </view>
        </view>

        <!-- 昵称 -->
        <view class="input-group">
          <text class="field-label">昵称</text>
          <view class="input-wrapper">
            <text class="material-symbols-outlined field-icon">face</text>
            <input v-model="form.nickname" placeholder="起个名字" class="input-box" placeholder-style="color:#abadaf;font-size:24rpx" />
          </view>
        </view>

        <!-- 手机号 -->
        <view class="input-group">
          <text class="field-label">手机号</text>
          <view class="input-wrapper">
            <text class="material-symbols-outlined field-icon">smartphone</text>
            <input v-model="form.phoneNumber" type="number" placeholder="输入手机号" class="input-box" placeholder-style="color:#abadaf;font-size:24rpx" />
          </view>
        </view>

        <!-- 修改密码展开 -->
        <view class="pwd-toggle-bar" @tap="showPwdFields = !showPwdFields">
          <view class="toggle-left">
            <text class="material-symbols-outlined toggle-icon">lock_reset</text>
            <text class="toggle-text">修改登录密码</text>
          </view>
          <text class="material-symbols-outlined arrow-icon" :class="{ 'rotated': showPwdFields }">expand_more</text>
        </view>

        <!-- 动态展开 -->
        <view v-if="showPwdFields" class="pwd-expanded">
          <view class="input-group">
            <text class="field-label">新密码</text>
            <view class="input-wrapper">
              <text class="material-symbols-outlined field-icon">lock_open</text>
              <input v-model="form.newPassword" type="password" placeholder="设置新密码" class="input-box" placeholder-style="color:#abadaf;font-size:24rpx" />
            </view>
          </view>
          <view class="input-group">
            <text class="field-label">确认新密码</text>
            <view class="input-wrapper">
              <text class="material-symbols-outlined field-icon">verified_user</text>
              <input v-model="form.confirmPassword" type="password" placeholder="再次确认" class="input-box" placeholder-style="color:#abadaf;font-size:24rpx" />
            </view>
          </view>
        </view>

        <!-- 保存按钮 -->
        <button class="save-btn" @tap="handleSave">
          <text class="btn-text">保存修改</text>
          <text class="material-symbols-outlined">done_all</text>
        </button>
      </view>

      <view class="safe-area-spacer"></view>
    </scroll-view>
  </view>
</template>

<script>
import { httpRequest } from '../../utils/api';

export default {
  data() {
    return {
      form: {
        id: null,
        username: '',
        nickname: '',
        phoneNumber: '',
        newPassword: '',
        confirmPassword: ''
      },
      showPwdFields: false
    };
  },
  onLoad() {
    this.loadUserInfo();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    loadUserInfo() {
      this.form.id = uni.getStorageSync('userId');
      this.form.username = uni.getStorageSync('username') || '';
      this.form.nickname = uni.getStorageSync('nickname') || '';
      this.form.phoneNumber = uni.getStorageSync('phoneNumber') || '';
    },
    async handleSave() {
      const { id, username, nickname, phoneNumber, newPassword, confirmPassword } = this.form;
      
      if (!id || !username) {
        uni.showToast({ title: '用户标识缺失', icon: 'none' });
        return;
      }
      if (!nickname) {
        uni.showToast({ title: '昵称不能为空', icon: 'none' });
        return;
      }
      if (!phoneNumber || !/^1[3-9]\d{9}$/.test(phoneNumber)) {
        uni.showToast({ title: '手机号格式有误', icon: 'none' });
        return;
      }

      if (this.showPwdFields) {
        if (!newPassword || newPassword.length < 6) {
          uni.showToast({ title: '密码需至少6位', icon: 'none' });
          return;
        }
        if (newPassword !== confirmPassword) {
          uni.showToast({ title: '两次密码不一致', icon: 'none' });
          return;
        }
      }

      try {
        uni.showLoading({ title: '保存中...', mask: true });
        const payload = {
          id,
          username,
          newNickname: nickname,
          newPhoneNumber: phoneNumber
        };
        if (this.showPwdFields && newPassword) {
          payload.newPassword = newPassword;
        }

        await httpRequest('/user/modify', 'PUT', payload);
        uni.hideLoading();
        uni.showToast({ title: '保存成功', icon: 'success' });
        
        uni.setStorageSync('nickname', nickname);
        uni.setStorageSync('phoneNumber', phoneNumber);
        
        setTimeout(() => {
          uni.navigateBack();
        }, 1200);
      } catch (error) {
        uni.hideLoading();
      }
    }
  }
};
</script>

<style scoped>
.padding {
  height: 64rpx; /* 顶部留白，适配状态栏 */
}
/* 容器布局 */
.profile-page { position: relative; min-height: 100vh; background-color: #f5f7f9; overflow: hidden; }
.bg-mesh { position: fixed; inset: 0; background: radial-gradient(at 0% 0%, #eef5ff 0%, #f5f7f9 100%); z-index: 0; }
.blob { position: fixed; border-radius: 50%; filter: blur(60px); opacity: 0.25; z-index: 1; }
.blob-1 { width: 300rpx; height: 300rpx; background: #6e9fff; top: -50rpx; left: -50rpx; }
.blob-2 { width: 400rpx; height: 400rpx; background: #6bff8f; bottom: 15%; right: -100rpx; }

/* 导航栏：缩小 */
.nav-bar { position: relative; z-index: 10; height: 90rpx; display: flex; align-items: center; justify-content: space-between; padding: 20rpx 30rpx 0; }
.back-btn { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; color: #0057bd; }
.nav-title { font-size: 30rpx; font-weight: 800; color: #1a1c1e; }
.spacer { width: 64rpx; }

/* 英雄区：尺寸优化 */
.scroll-container { position: relative; z-index: 5; height: calc(100vh - 90rpx); }
.hero-section { padding: 30rpx 48rpx 24rpx; }
.hero-title { font-size: 48rpx; font-weight: 800; color: #1a1c1e; line-height: 1.1; }
.primary { color: #0057bd; }
.hero-desc { font-size: 24rpx; color: #595c5e; margin-top: 10rpx; display: block; }

/* 卡片 */
.form-card { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(20px); border-radius: 36rpx; margin: 10rpx 28rpx 40rpx; padding: 36rpx; box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.04); }
.input-group { margin-bottom: 24rpx; }
.field-label { font-size: 20rpx; font-weight: 700; color: #595c5e; margin-left: 10rpx; margin-bottom: 8rpx; display: block; text-transform: uppercase; }
.input-wrapper { background: #ffffff; height: 88rpx; border-radius: 20rpx; display: flex; align-items: center; padding: 0 24rpx; border: 2rpx solid #eff1f3; }
.disabled-box { background: #f1f4f8; border: none; }
.field-icon { color: #0057bd; opacity: 0.4; font-size: 36rpx; }
.input-box { flex: 1; margin-left: 16rpx; font-size: 26rpx; color: #1a1c1e; }

/* 密码开关 */
.pwd-toggle-bar { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 10rpx; margin-bottom: 10rpx; }
.toggle-left { display: flex; align-items: center; gap: 12rpx; }
.toggle-icon { color: #0057bd; font-size: 34rpx; }
.toggle-text { font-size: 24rpx; font-weight: 700; color: #0057bd; }
.arrow-icon { color: #0057bd; font-size: 32rpx; transition: transform 0.3s; }
.rotated { transform: rotate(180deg); }

.pwd-expanded { margin-top: 10rpx; }

/* 保存按钮 */
.save-btn { background: #0057bd; height: 92rpx; border-radius: 46rpx; display: flex; align-items: center; justify-content: center; gap: 10rpx; margin-top: 40rpx; border: none; }
.btn-text { color: #fff; font-size: 28rpx; font-weight: 700; }

.safe-area-spacer { height: 60rpx; }
</style>