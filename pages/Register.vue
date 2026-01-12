<template>
  <view class="register-page">
    <!-- 背景图 -->
    <image
      class="bg-image"
      src="/static/images/leaves-bg.png"
      mode="cover"
    ></image>

    <!-- 返回按钮 -->
    <navigator url="/pages/Login" class="back-btn"> </navigator>

    <!-- 表单区域 -->
    <view class="form-container">
      <!-- 账号 -->
      <view class="form-item">
        <div class="form-top">
          <div
            class="icon"
            style="background-image: url('../static/Login/user.png')"
          ></div>
          <text>账号</text>
        </div>
        <input
          v-model="formData.username"
          placeholder="请输入账号"
          class="input-field"
        />
      </view>
      <view class="input-border"></view>
      
      <!-- 昵称 -->
      <view class="form-item">
        <div class="form-top">
          <div
            class="icon"
            style="background-image: url('../static/Login/user.png')"
          ></div>
          <text>昵称</text>
        </div>
        <input
          v-model="formData.nickname"
          placeholder="请输入昵称"
          class="input-field"
        />
      </view>
      <view class="input-border"></view>

      <!-- 密码 -->
      <view class="form-item">
        <div class="form-top">
          <div
            class="icon"
            style="background-image: url('../static/Login/pwd.png')"
          ></div>
          <text>密码</text>
        </div>
        <div style="display: flex; align-items: center">
          <input
            v-model="formData.password"
            :type="PwdAble ? 'text' : 'password'"
            placeholder="请设置8-16位密码"
            placeholder-class="placeholder"
            class="input-field"
          />
          <div
            class="eye-icon"
            v-if="PwdAble"
            style="background-image: url('../static/Register/pwd-able.png')"
            @click="PwdAble = !PwdAble"
          ></div>
          <div
            class="eye-icon"
            v-if="!PwdAble"
            style="background-image: url('../static/Register/pwd-disable.png')"
            @click="PwdAble = !PwdAble"
          ></div>
        </div>
      </view>
      <view class="input-border"></view>

      <!-- 确认密码 -->
      <view class="form-item">
        <div class="form-top">
          <div
            class="icon"
            style="background-image: url('../static/Login/pwd.png')"
          ></div>
          <text>确认密码</text>
        </div>
        <div style="display: flex; align-items: center">
          <input
            v-model="formData.confirmPassword"
            :type="ConPwdAble ? 'text' : 'password'"
            placeholder="请设置8-16位密码"
            placeholder-class="placeholder"
            class="input-field"
          />
          <div
            class="eye-icon"
            v-if="ConPwdAble"
            style="background-image: url('../static/Register/pwd-able.png')"
            @click="ConPwdAble = !ConPwdAble"
          ></div>
          <div
            class="eye-icon"
            v-if="!ConPwdAble"
            style="background-image: url('../static/Register/pwd-disable.png')"
            @click="ConPwdAble = !ConPwdAble"
          ></div>
        </div>
      </view>
      <view class="input-border"></view>

      <!-- 注册按钮 -->
      <button class="register-btn" @click="handleRegister">注册</button>
    </view>
  </view>
</template>

<script>
import registerAPI from "../api/login.js";

export default {
  data() {
    return {
      formData: {
        username: "",
        password: "",
        confirmPassword: "",
        nickname: ""
      },
      PwdAble: false,
      ConPwdAble: false,
    };
  },
  methods: {
    async handleRegister() {
      // 验证逻辑（示例）
      if (!this.formData.username) {
        uni.showToast({ title: "请输入账号", icon: "none" });
        return;
      }
      if (!this.formData.password || this.formData.password.length < 8) {
        uni.showToast({ title: "密码需8-16位", icon: "none" });
        return;
      }
      if (this.formData.password !== this.formData.confirmPassword) {
        uni.showToast({ title: "两次密码不一致", icon: "none" });
        return;
      }

      try {
        // 调用登录API
        const result = await registerAPI.register({
          username: this.formData.username.trim(),
          password: this.formData.password,
          nickname: this.formData.nickname
        });

        // 登录成功处理
        uni.showToast({
          title: "登录成功",
          icon: "success",
        });

        // 保存token
        if (result.token) {
          uni.setStorageSync("token", result.token);
        }

        // 跳转到首页
        uni.showToast({ title: "注册成功", icon: "success" });
        setTimeout(() => {
          uni.switchTab({ url: "/pages/index/index" });
        }, 1000);
      } catch (error) {
        console.error("注册失败:", error);
        // 错误信息已经在API拦截器中处理了
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>

<style scoped>
.register-page {
  position: absolute;
  width: 100%;
  height: 100vh;
  background-image: url("../static/Register/background.png");
  background-size: cover;
  background-position: center;
}

.bg-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  opacity: 0.3;
}

.back-btn {
  position: absolute;
  top: 20rpx;
  left: 20rpx;
  z-index: 10;

  background-image: url("../static/Register/back.png");
  width: 36rpx;
  height: 36rpx;
  background-size: cover;
}

.form-container {
  margin-top: 150rpx;
  padding: 0 40rpx;
  width: 100%;
  box-sizing: border-box;
}

.form-item {
  display: flex;
  flex-direction: column;
  margin-top: 40rpx;
  position: relative;
}

.form-top {
  display: flex;
  align-items: center;

  font-size: 32rpx;
}

.icon {
  width: 64rpx;
  height: 64rpx;
  /* margin: auto; */

  background-size: cover;
}

.input-field {
  /* flex: 1; */
  width: 80%;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 32rpx;
  color: #333;
  padding: 0;
  outline: none;

  margin-left: auto;
  margin-right: auto;
}

.input-border {
  width: 100%;
  height: 2rpx;
  background: #000;
  /* transition: all 0.3s; */
}

.eye-icon {
  position: absolute;
  right: 10rpx;

  width: 36rpx;
  height: 36rpx;
  background-size: cover;
}

.register-btn {
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
  margin-top: 100rpx;
  transition: all 0.3s;
}
</style>
