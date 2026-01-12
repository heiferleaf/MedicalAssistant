<template>
  <view class="login-container">
    <!-- 背景装饰 -->
    <view class="background">
      <view class="back-pic"></view>
      <view class="back-white"></view>
    </view>

    <!-- 主内容区域 -->
    <view class="login-content">
      <!-- 登录表单 -->
      <view class="form-container" v-if="isLogin">
        <view class="form-item">
          <div
            class="form-label"
            style="background-image: url('../static/Login/user.png')"
          ></div>
          <input
            class="form-input"
            v-model="loginForm.username"
            placeholder="请输入账号"
            placeholder-class="placeholder"
            type="text"
            @focus="onInputFocus"
            @blur="onInputBlur"
          />
          <view class="input-border"></view>
        </view>

        <view class="form-item">
          <div
            class="form-label"
            style="background-image: url('../static/Login/pwd.png')"
          ></div>
          <input
            class="form-input"
            v-model="loginForm.password"
            placeholder="请输入密码"
            placeholder-class="placeholder"
            :password="!showLoginPassword"
            @focus="onInputFocus"
            @blur="onInputBlur"
          />
          <view class="input-border"></view>
        </view>

        <!-- 记住密码和忘记密码 -->
        <view class="form-options">
          <view class="forget-password" @tap="goForgetPassword">
            <text class="option-text">找回密码</text>
          </view>
        </view>

        <!-- 登录按钮 -->
        <button
          class="submit-btn"
          :class="{ disabled: !isLoginFormValid }"
          @tap="handleLogin"
        >
          <text v-if="!loading">登录</text>
          <text v-else class="loading-text">登录中...</text>
        </button>
      </view>

      <!-- 底部跳转 -->
      <view class="bottom-link">
        <text v-if="isLogin">还没有账号？</text>
        <text v-else>已有账号？</text>
        <text
          class="link-text"
          @tap="
            uni.navigateTo({
              url: '/pages/Register',
            })
          "
        >
          {{ isLogin ? "立即注册" : "立即登录" }}
        </text>
      </view>
    </view>
  </view>
</template>

<script>
// 引入API模块
import loginAPI from "../api/login.js";

export default {
  data() {
    return {
      isLogin: true, // 当前是登录还是注册
      loading: false, // 加载状态

      // 登录表单
      loginForm: {
        username: "",
        password: "",
      },
      showLoginPassword: false, // 显示登录密码
      showConfirmPassword: false, // 显示确认密码
    };
  },

  computed: {
    // 用户名验证（字母、数字、下划线，3-16位）
    isUsernameValid() {
      const reg = /^[a-zA-Z0-9_]{3,16}$/;
      return reg.test(this.registerForm.username);
    },

    // 密码验证（字母+数字，6-20位）
    isPasswordValid() {
      const reg = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/;
      return reg.test(this.registerForm.password);
    },

    // 确认密码验证
    isConfirmPasswordValid() {
      return this.registerForm.password === this.registerForm.confirmPassword;
    },

    // 登录表单验证
    isLoginFormValid() {
      return (
        this.loginForm.username.trim() !== "" &&
        this.loginForm.password.trim() !== ""
      );
    },
  },

  onLoad() {
    // 页面加载时检查是否有记住的账号
    this.checkRememberedAccount();
  },

  methods: {
    // 检查记住的账号
    checkRememberedAccount() {
      try {
        const remembered = uni.getStorageSync("rememberedAccount");
        if (remembered) {
          this.loginForm.username = remembered.username || "";
          this.loginForm.password = remembered.password || "";
          this.rememberMe = true;
        }
      } catch (e) {
        console.error("读取记住的账号失败:", e);
      }
    },

    // 切换密码可见性
    toggleLoginPassword() {
      this.showLoginPassword = !this.showLoginPassword;
    },

    toggleConfirmPassword() {
      this.showConfirmPassword = !this.showConfirmPassword;
    },

    // 切换记住密码
    toggleRemember() {
      this.rememberMe = !this.rememberMe;
    },

    // 登录
    async handleLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        uni.showToast({
          title: "请输入用户名和密码",
          icon: "error",
        });
        return;
      }
      if (!this.isLoginFormValid || this.loading) return;

      this.loading = true;

      try {
        // 调用登录API
        // const result = await loginAPI.login({
        //   username: this.loginForm.username.trim(),
        //   password: this.loginForm.password,
        // });

        // 登录成功处理
        uni.showToast({
          title: "登录成功",
          icon: "success",
        });

        // 保存token
        // if (result.token) {
        //   uni.setStorageSync("token", result.token);
        // }

        // 记住密码
        if (this.rememberMe) {
          uni.setStorageSync("rememberedAccount", {
            username: this.loginForm.username.trim(),
            password: this.loginForm.password,
          });
        } else {
          uni.removeStorageSync("rememberedAccount");
        }

        // 跳转到首页
        setTimeout(() => {
          uni.navigateTo({
            url: "/pages/index/index",
          });
        }, 1500);
      } catch (error) {
        console.error("登录失败:", error);
        // 错误信息已经在API拦截器中处理了
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.background {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  overflow: hidden;
  z-index: 0;
}

.back-pic {
  width: 100%;
  height: 60%;
  background-image: url("/static/Login/loginback.png");
  background-size: cover;
  background-position: center;
}

.back-white {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 70%;
  /* 创建渐变消失效果 */
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, white 30%);
  mask-image: linear-gradient(to bottom, transparent 0%, white 30%);
  background-color: white;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 300rpx;
  height: 300rpx;
  top: -150rpx;
  right: -150rpx;
}

.circle-2 {
  width: 200rpx;
  height: 200rpx;
  bottom: 100rpx;
  left: -100rpx;
}

.circle-3 {
  width: 150rpx;
  height: 150rpx;
  bottom: -75rpx;
  right: 20%;
}

.login-content {
  width: 100%;
  max-width: 600rpx;
  margin-top: 400rpx;
  padding: 60rpx 50rpx;
  //   position: relative;
  z-index: 1;
}

.tab-container {
  display: flex;
  margin-bottom: 50rpx;
  border-bottom: 2rpx solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  font-size: 32rpx;
  color: #999;
  position: relative;
  transition: color 0.3s;
}

.tab-item.active {
  color: #667eea;
  font-weight: bold;
}

.tab-indicator {
  position: absolute;
  bottom: -2rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 4rpx;
  background: #667eea;
  border-radius: 2rpx;
}

.form-container {
  margin-bottom: 40rpx;
}

.form-item {
  display: flex;
  margin-bottom: 40rpx;
  position: relative;
}

.form-label {
  width: 64rpx;
  height: 64rpx;
  margin: auto;

  background-size: cover;
}

.form-input {
  width: 100%;
  height: 80rpx;
  border-bottom: solid 4rpx black;
  padding: 0 30rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
  transition: all 0.3s;
}

.form-input:focus {
  background: #fff;
  box-shadow: 0 0 0 2rpx rgba(102, 126, 234, 0.2);
}

.placeholder {
  color: #aaa;
  font-size: 28rpx;
}

.input-border {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2rpx;
  background: #000;
  transition: all 0.3s;
}

.form-input:focus + .input-border {
  height: 4rpx;
  background: #667eea;
}

.password-toggle {
  position: absolute;
  right: 30rpx;
  top: 55rpx;
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.password-toggle .icon {
  font-size: 32rpx;
}

.input-tips {
  margin-top: 10rpx;
  padding: 0 10rpx;
}

.tips-text {
  font-size: 24rpx;
  color: #ff4444;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40rpx;
}

.remember-me,
.forget-password {
  display: flex;
  align-items: center;
}

.checkbox {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid #ddd;
  border-radius: 6rpx;
  margin-right: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.checkbox.checked {
  background: #667eea;
  border-color: #667eea;
}

.checkmark {
  color: white;
  font-size: 24rpx;
}

.option-text {
  font-size: 26rpx;
  color: #666;
}

.forget-password .option-text {
  color: #666;
}

.submit-btn {
  width: 64%;
  height: 88rpx;
  background-color: #000;
  color: white;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20rpx;
  transition: all 0.3s;
}

.submit-btn.disabled {
  //   opacity: 0.9;
  box-shadow: none;
}

.loading-text {
  display: flex;
  align-items: center;
}

.loading-text::after {
  content: "";
  width: 20rpx;
  height: 20rpx;
  border: 2rpx solid white;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-left: 15rpx;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.bottom-link {
  text-align: center;
  font-size: 28rpx;
  color: #666;
  margin-top: 40rpx;
}

.link-text {
  color: #667eea;
  font-weight: 500;
  margin-left: 10rpx;
}
</style>
