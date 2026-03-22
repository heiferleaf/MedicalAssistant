<template>
  <view class="login-container">
    <view class="illustration-box">
      <image
        src="../static/Login/image.png"
        mode="aspectFill" class="bg-image"></image>
      <view class="mask-gradient"></view>
    </view>

    <view class="form-wrapper">
      <view class="header-section">
        <text class="title">欢迎回来</text>
        <text class="subtitle">请登录您的医疗助手账号</text>
      </view>

      <view class="form-content">
        <view class="input-group">
          <view class="icon-box">
            <text class="iconfont icon-person"></text>
          </view>
          <input class="input-field" type="text" placeholder="请输入账号" v-model="loginForm.username"
            placeholder-class="placeholder-style" />
        </view>

        <view class="input-group">
          <view class="icon-box">
            <text class="iconfont icon-lock"></text>
          </view>
          <input class="input-field" :password="!showLoginPassword" placeholder="请输入密码" v-model="loginForm.password"
            placeholder-class="placeholder-style" />
          <view class="eye-box" @tap="toggleLoginPassword">
            <text class="iconfont" :class="showLoginPassword ? 'icon-eye-open' : 'icon-eye-close'"></text>
          </view>
        </view>

        <view class="action-bar">
          <view class="remember-me" @tap="toggleRemember">
            <view class="checkbox" :class="{ 'checked': rememberMe }">
              <text v-if="rememberMe" class="iconfont icon-check"></text>
            </view>
            <text class="remember-text">记住密码</text>
          </view>
        </view>

        <view class="btn-section">
          <button class="login-btn" :loading="loading" :disabled="loading" @tap="handleLogin">
            登录
          </button>
        </view>
      </view>

      <view class="footer-section">
        <text class="footer-text">还没有账号？</text>
        <text class="register-link" @click="uni.navigateTo({ url: 'Register' })">立即注册</text>
      </view>
    </view>

    <view class="bottom-bar"></view>
  </view>
</template>

<script>
// 引入API模块
import loginAPI from "../api/login.js";
import { connect } from '../config/config';
export default {
  data() {
    return {
      isLogin: true,
      loading: false,
      rememberMe: false, // 补充了逻辑中用到的变量
      loginForm: {
        username: "",
        password: "",
      },
      showLoginPassword: false,
    };
  },
  computed: {
    isLoginFormValid() {
      return (
        this.loginForm.username.trim() !== "" &&
        this.loginForm.password.trim() !== ""
      );
    },
  },
  onLoad() {
    this.checkRememberedAccount();
  },
  methods: {
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
    toggleLoginPassword() {
      this.showLoginPassword = !this.showLoginPassword;
    },
    toggleRemember() {
      this.rememberMe = !this.rememberMe;
    },
    async handleLogin() {
      // uni.navigateTo({
      //   url: "/pages/index/index",
      // });
      // return;
      if (!this.loginForm.username || !this.loginForm.password) {
        uni.showToast({
          title: "请输入用户名和密码",
          icon: "none",
        });
        return;
      }
      if (this.loading) return;

      this.loading = true;
      try {
        const result = await loginAPI.login({
          username: this.loginForm.username.trim(),
          password: this.loginForm.password,
        });

        uni.showToast({
          title: "登录成功",
          icon: "success",
        });
        console.log("登录结果:", result);

        uni.setStorageSync("userId", result.id);
        uni.setStorageSync("username", result.username);
        uni.setStorageSync("nickname", result.nickname);
        uni.setStorageSync("phoneNumber", result.phoneNumber);
        uni.setStorageSync("accessToken", result.accessToken);
        uni.setStorageSync("refreshToken", result.refreshToken);
        uni.setStorageSync("userName", this.loginForm.username.trim());
        uni.setStorageSync("createTime", result.createTime);

        connect(); 

        // console.log("保存的用户名:",this.loginForm.username.trim());

        setTimeout(() => {
          uni.navigateTo({
            url: "/pages/index/index",
          });
        }, 1500);
      } catch (error) {
        console.error("登录失败:", error);
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
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;

  /* 暗黑模式适配 */
  @media (prefers-color-scheme: dark) {
    background-color: #0f172a;
  }
}

.illustration-box {
  position: relative;
  width: 100%;
  height: 40vh;
  overflow: hidden;

  .bg-image {
    width: 100%;
    height: 100%;
  }

  .mask-gradient {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 40%;
    background: linear-gradient(to bottom, transparent, #f8fafc);

    @media (prefers-color-scheme: dark) {
      background: linear-gradient(to bottom, transparent, #0f172a);
    }
  }
}

.form-wrapper {
  flex: 1;
  padding: 0 60rpx;
  margin-top: -60rpx;
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
}

.header-section {
  margin-bottom: 60rpx;

  .title {
    font-size: 56rpx;
    font-weight: bold;
    color: #1e293b;
    display: block;
    margin-bottom: 12rpx;

    @media (prefers-color-scheme: dark) {
      color: #ffffff;
    }
  }

  .subtitle {
    font-size: 28rpx;
    color: #64748b;
  }
}

.input-group {
  display: flex;
  align-items: center;
  border-bottom: 2rpx solid #e2e8f0;
  margin-bottom: 40rpx;
  padding: 20rpx 0;
  transition: all 0.3s;

  @media (prefers-color-scheme: dark) {
    border-color: #334155;
  }

  .icon-box {
    padding: 0 20rpx;
    color: #94a3b8;
  }

  .input-field {
    flex: 1;
    font-size: 32rpx;
    color: #1e293b;

    @media (prefers-color-scheme: dark) {
      color: #ffffff;
    }
  }
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 60rpx;

  .remember-me {
    display: flex;
    align-items: center;

    .checkbox {
      width: 32rpx;
      height: 32rpx;
      border: 2rpx solid #cbd5e1;
      border-radius: 6rpx;
      margin-right: 12rpx;
      display: flex;
      align-items: center;
      justify-content: center;

      &.checked {
        background-color: #6366f1;
        border-color: #6366f1;
      }
    }

    .remember-text {
      font-size: 26rpx;
      color: #64748b;
    }
  }

  .forget-pwd {
    font-size: 26rpx;
    color: #64748b;
  }
}

.login-btn {
  width: 100%;
  height: 100rpx;
  line-height: 100rpx;
  background-color: #0f172a;
  color: #ffffff;
  border-radius: 50rpx;
  font-weight: bold;
  font-size: 32rpx;
  box-shadow: 0 10rpx 20rpx rgba(15, 23, 42, 0.2);

  @media (prefers-color-scheme: dark) {
    background-color: #ffffff;
    color: #0f172a;
  }

  &:active {
    transform: scale(0.98);
  }
}

.footer-section {
  margin-top: auto;
  padding-bottom: 60rpx;
  text-align: center;

  .footer-text {
    color: #64748b;
    font-size: 28rpx;
  }

  .register-link {
    color: #6366f1;
    font-weight: 500;
    margin-left: 10rpx;
    font-size: 28rpx;
  }
}

.bottom-bar {
  width: 240rpx;
  height: 8rpx;
  background-color: #e2e8f0;
  border-radius: 4rpx;
  margin: 0 auto 20rpx;
  opacity: 0.5;

  @media (prefers-color-scheme: dark) {
    background-color: #334155;
  }
}

/* 这里需要你项目中配置了 Iconfont */
.iconfont {
  font-family: "Material Icons";
  /* 如果你用的是原HTML的字体图标 */
  font-size: 40rpx;
}
</style>