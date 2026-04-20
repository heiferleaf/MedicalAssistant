<template>
  <view class="register-page">
    <view class="padding"></view>
    <view class="bg-mesh"></view>
    
    <!-- 顶部导航调小高度 -->
    <view class="nav-bar">
      <view class="back-btn" @tap="goBack">
        <text class="material-symbols-outlined">arrow_back</text>
      </view>
      <text class="nav-title">药无忧</text>
      <view class="spacer"></view>
    </view>

    <scroll-view scroll-y class="scroll-container" :show-scrollbar="false">
      <!-- 调小英雄区间距和字体 -->
      <view class="hero-section">
        <text class="hero-title">开启您的<text class="primary">\n健康之旅</text></text>
        <text class="hero-desc">创建账户以享受智能医疗咨询服务</text>
      </view>

      <!-- 调小卡片内边距和圆角 -->
      <view class="form-card">
        <view class="input-group">
          <text class="field-label">账号</text>
          <view class="input-wrapper">
            <text class="material-symbols-outlined field-icon">account_circle</text>
            <input v-model="formData.username" placeholder="输入您的账号名" class="input-box" placeholder-style="color:#abadaf;font-size:26rpx" />
          </view>
        </view>

        <view class="input-group">
          <text class="field-label">昵称</text>
          <view class="input-wrapper">
            <text class="material-symbols-outlined field-icon">face</text>
            <input v-model="formData.nickname" placeholder="起一个好听的名字" class="input-box" placeholder-style="color:#abadaf;font-size:26rpx" />
          </view>
        </view>

        <view class="input-group">
          <text class="field-label">手机号</text>
          <view class="input-wrapper">
            <text class="material-symbols-outlined field-icon">smartphone</text>
            <input v-model="formData.phoneNumber" type="number" placeholder="输入11位手机号" class="input-box" placeholder-style="color:#abadaf;font-size:26rpx" />
          </view>
        </view>

        <view class="input-group">
          <text class="field-label">密码</text>
          <view class="input-wrapper">
            <text class="material-symbols-outlined field-icon">lock</text>
            <input v-model="formData.password" :type="showPwd ? 'text' : 'password'" placeholder="请输入密码" class="input-box" placeholder-style="color:#abadaf;font-size:26rpx" />
            <text class="material-symbols-outlined eye-icon" @tap="showPwd = !showPwd">{{ showPwd ? 'visibility' : 'visibility_off' }}</text>
          </view>
        </view>

        <view class="input-group">
          <text class="field-label">确认密码</text>
          <view class="input-wrapper">
            <text class="material-symbols-outlined field-icon">verified_user</text>
            <input v-model="formData.confirmPassword" :type="showConPwd ? 'text' : 'password'" placeholder="再次确认密码" class="input-box" placeholder-style="color:#abadaf;font-size:26rpx" />
          </view>
        </view>

        <!-- 调小协议区域 -->
        <view class="agreement-row">
          <view class="checkbox-outer" :class="{ 'agreed': agreed }" @tap="agreed = !agreed">
            <text v-if="agreed" class="material-symbols-outlined check-mark">check</text>
          </view>
          <view class="agreement-text">
            <text @tap="agreed = !agreed">我已阅读并同意 </text>
            <text class="link-span" @tap="showPrivacyPopup = true">服务协议</text>
            <text @tap="agreed = !agreed"> 与 </text>
            <text class="link-span" @tap="showPrivacyPopup = true">隐私权政策</text>
          </view>
        </view>

        <!-- 注册按钮 -->
        <button class="register-btn" @tap="handleRegister">
          <text class="btn-text">注册</text>
          <text class="material-symbols-outlined btn-arrow">arrow_forward</text>
        </button>
      </view>

      <view class="footer-link">
        <text class="footer-gray">已有账号? </text>
        <text class="footer-blue" @tap="goLogin">立即登录</text>
      </view>
      
      <view class="safe-area-spacer"></view>
    </scroll-view>

    <!-- 隐私政策/服务协议弹窗 -->
    <view class="modal-overlay" v-if="showPrivacyPopup">
      <view class="modal-card">
        <text class="modal-title">服务协议与隐私政策</text>
        <scroll-view scroll-y class="modal-content">
          <text class="modal-text">我们非常重视您的个人信息和隐私保护。在您首次使用我们的服务前，请仔细阅读我们的隐私权政策。</text>
          
          <view class="modal-section-spacing"></view>
          
          <text class="modal-text primary-text bold-text">为向您提供完整的服务，我们集成了以下第三方SDK：</text>
          
          <view class="modal-section-spacing"></view>
          
          <view class="flex-column">
            <text class="modal-subtitle">第三方SDK名称：</text>
            <text class="modal-text">OPPO健康服务SDK</text>
            
            <view class="modal-section-spacing"></view>

            <text class="modal-subtitle">第三方公司名称：</text>
            <text class="modal-text">广东欢太科技有限公司</text>
            
            <view class="modal-section-spacing"></view>

            <text class="modal-subtitle">收集个人信息种类：</text>
            <text class="modal-text">应用基本信息（健康APP包名、应用商店包名）</text>
            
            <view class="modal-section-spacing"></view>

            <text class="modal-subtitle">使用目的：</text>
            <text class="modal-text">用于判断应用安装状态，以支持APP端与第三方数据传输</text>
            
            <view class="modal-section-spacing"></view>

            <text class="modal-subtitle">隐私政策链接：</text>
            <text class="modal-link" @tap="openPrivacyLink">查看OPPO隐私政策</text>
          </view>
        </scroll-view>
        <view class="modal-actions">
          <button class="modal-btn-secondary" @tap="handlePrivacy(false)">不同意</button>
          <button class="modal-btn-primary" @tap="handlePrivacy(true)">同意</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import registerAPI from "../api/login.js";
export default {
  data() {
    return {
      formData: { username: "", password: "", confirmPassword: "", nickname: "", phoneNumber: "" },
      showPwd: false, showConPwd: false, agreed: false,
      showPrivacyPopup: false
    };
  },
  methods: {
    goBack() { uni.redirectTo({ url: `/pages/Login?username=${this.formData.username}&password=${this.formData.password}` }); },
    goLogin() { uni.redirectTo({ url: '/pages/Login' }); },
    handlePrivacy(agreed) {
        this.agreed = agreed;
        this.showPrivacyPopup = false;
    },
    openPrivacyLink() {
        // Here you'd open the link normally via a web-view or uni method
        uni.showToast({ title: "即将跳转浏览器查看", icon: "none" });
    },
    async handleRegister() {
      if (!this.agreed) { uni.showToast({ title: "请先同意协议", icon: "none" }); return; }
      if (!this.formData.username) { uni.showToast({ title: "请输入账号", icon: "none" }); return; }
      if (!/^1[3-9]\d{9}$/.test(this.formData.phoneNumber)) { uni.showToast({ title: "手机号格式错误", icon: "none" }); return; }
      if (this.formData.password.length < 8) { uni.showToast({ title: "密码需8位以上", icon: "none" }); return; }
      if (this.formData.password !== this.formData.confirmPassword) { uni.showToast({ title: "两次密码不一致", icon: "none" }); return; }
      try {
        await registerAPI.register(this.formData);
        uni.showToast({ title: "注册成功", icon: "success" });
        setTimeout(() => { uni.navigateBack(); }, 1000);
      } catch (e) { console.error(e); }
    }
  }
};
</script>

<style scoped>
.padding {
  height: 64rpx; /* 顶部留白，适配状态栏 */
}
/* 整体尺寸优化 */
.register-page { position: relative; min-height: 100vh; background-color: #f5f7f9; }
.bg-mesh { position: fixed; inset: 0; background: radial-gradient(at 0% 0%, #eef2ff 0%, #f5f7f9 100%); z-index: 0; }

.nav-bar { position: relative; z-index: 10; height: 100rpx; display: flex; align-items: center; justify-content: space-between; padding: 20rpx 30rpx 0; }
.back-btn { width: 70rpx; height: 70rpx; display: flex; align-items: center; justify-content: center; color: #0057bd; }
.nav-title { font-size: 32rpx; font-weight: 700; color: #2c2f31; }
.spacer { width: 70rpx; }

.hero-section { padding: 30rpx 50rpx 40rpx; }
.hero-title { font-size: 56rpx; font-weight: 800; color: #2c2f31; line-height: 1.1; }
.primary { color: #0057bd; }
.hero-desc { font-size: 26rpx; color: #595c5e; margin-top: 16rpx; display: block; }

.form-card { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(20px); border-radius: 40rpx; margin: 0 32rpx; padding: 40rpx; box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.03); }
.input-group { margin-bottom: 28rpx; }
.field-label { font-size: 20rpx; font-weight: 700; color: #595c5e; margin-left: 10rpx; margin-bottom: 10rpx; display: block; }
.input-wrapper { background: #ffffff; height: 96rpx; border-radius: 24rpx; display: flex; align-items: center; padding: 0 24rpx; border: 2rpx solid #eff1f3; }
.field-icon { color: #0057bd; opacity: 0.45; font-size: 40rpx; }
.input-box { flex: 1; margin-left: 16rpx; font-size: 28rpx; color: #2c2f31; }
.eye-icon { color: #abadaf; font-size: 36rpx; }

.agreement-row { display: flex; align-items: center; margin: 24rpx 0 40rpx; padding-left: 6rpx; }
.checkbox-outer { width: 36rpx; height: 36rpx; border: 3rpx solid #cbd5e1; border-radius: 10rpx; display: flex; align-items: center; justify-content: center; }
.agreed { background: #0057bd; border-color: #0057bd; }
.check-mark { color: #fff; font-size: 24rpx; font-weight: bold; }
.agreement-text { font-size: 24rpx; color: #2c2f31; margin-left: 16rpx; }
.link-span { color: #0057bd; font-weight: 700; margin: 0 4rpx; }

.register-btn { background: #0057bd; height: 100rpx; border-radius: 50rpx; display: flex; align-items: center; justify-content: center; gap: 10rpx; border: none; box-shadow: 0 10rpx 24rpx rgba(0, 87, 189, 0.2); }
.btn-text { color: #fff; font-size: 30rpx; font-weight: 700; }
.btn-arrow { color: #fff; font-size: 34rpx; }

.footer-link { text-align: center; margin: 40rpx 0; }
.footer-gray { font-size: 26rpx; color: #595c5e; }
.footer-blue { font-size: 26rpx; color: #0057bd; font-weight: 700; }
.safe-area-spacer { height: 40rpx; }

/* Modal Styles based on Frontend Design System Rules */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  width: 80%;
  max-width: 320px; /* Maximum limit */
  background: #ffffff;
  border-radius: 8px; /* Following rules (6px or 8px) */
  box-shadow: 0 4px 12px rgba(0,0,0,0.1); /* Shadow level 2 */
  padding: 24px;      /* Consistent 4px grid spacing */
  display: flex;
  flex-direction: column;
}

.modal-title {
  font-size: 20px;   /* Allowed font scale */
  font-weight: 600;
  line-height: 1.25;
  color: #111827;    /* Primary text */
  margin-bottom: 16px; 
  text-align: center;
}

.modal-content {
  max-height: 480px; 
  margin-bottom: 24px; 
}

.modal-section-spacing {
  height: 16px;
}

.modal-subtitle {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.25;
  color: #111827;
  display: block;
}

.modal-text {
  font-size: 14px;
  font-weight: 400;
  line-height: 1.5;
  color: #6b7280;   /* Secondary text */
  display: block;
  margin-top: 4px;
}

.primary-text {
  color: #111827;
}

.bold-text {
  font-weight: 600;
}

.modal-link {
  font-size: 14px;
  font-weight: 400;
  line-height: 1.5;
  color: #0ea5e9;  /* Using a brand color to replace blue-purple */
  text-decoration: underline;
  display: block;
  margin-top: 4px;
}

.modal-actions {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.modal-btn-secondary {
  flex: 1;
  background: transparent;
  border: 1px solid #d1d5db; /* Primary border color */
  border-radius: 6px;
  height: 48px;
  color: #111827;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-btn-primary {
  flex: 1;
  background: #0ea5e9;
  border: none;
  border-radius: 6px;
  height: 48px;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>