<template>
  <view class="invite-container">
    <view class="card">
      <view class="title">添加家庭成员</view>
      <view class="subtitle">通过手机号邀请对方加入您的家庭组</view>

      <view class="input-group">
        <text class="label">手机号码</text>
        <input class="custom-input" type="number" v-model="phoneNumber" placeholder="请输入对方绑定的手机号"
          placeholder-style="color:#ccc" />
      </view>

      <view class="input-group">
        <text class="label">备注信息</text>
        <textarea class="custom-textarea" v-model="remark" placeholder="例如：我是你的孩子，快通过我的申请吧~"
          placeholder-style="color:#ccc" />
      </view>

      <button class="submit-btn" @click="handleInvite">发送邀请</button>
    </view>

    <view class="footer-tips">
      <text>· 对方将在“我的消息”中收到您的邀请</text>
      <text>· 只有对方同意后才会正式进入家庭组</text>
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
          uni.showToast({ title: res.message || '发送失败', icon: 'none' });
        }
      } catch (e) {
        console.log("报错显示:", e);
        uni.showToast({ title: '服务器繁忙', icon: 'none' });
      } finally {
        uni.hideLoading();
      }
    }
  }
};
</script>

<style scoped>
.invite-container {
  padding: 40rpx;
  background-color: #f8f9fb;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.card {
  background-color: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.05);

  margin-top: auto;
  margin-bottom: auto;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 8rpx;
}

.subtitle {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 40rpx;
}

.input-group {
  margin-bottom: 30rpx;
}

.label {
  font-size: 28rpx;
  color: #666;
  font-weight: 500;
  margin-bottom: 16rpx;
  display: block;
}

.custom-input {
  background-color: #f5f6f7;
  height: 90rpx;
  border-radius: 12rpx;
  padding: 0 30rpx;
  font-size: 30rpx;
}

.custom-textarea {
  background-color: #f5f6f7;
  width: auto;
  height: 200rpx;
  border-radius: 12rpx;
  padding: 24rpx 30rpx;
  font-size: 28rpx;
  line-height: 1.5;
}

.submit-btn {
  margin-top: 40rpx;
  background-color: #007AFF;
  color: #fff;
  border-radius: 50rpx;
  height: 90rpx;
  line-height: 90rpx;
  font-size: 32rpx;
  border: none;
}

.submit-btn:active {
  opacity: 0.8;
}

.footer-tips {
  margin-top: auto;
  padding-bottom: 60rpx;
  text-align: center;
}

.footer-tips text {
  display: block;
  font-size: 22rpx;
  color: #bbb;
  line-height: 1.8;
}
</style>