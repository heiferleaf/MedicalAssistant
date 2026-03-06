<template>
  <view class="create-container">
    <view class="header-section">
      <text class="main-title">创建家庭组</text>
      <text class="tips">建立一个私密的健康管理空间，邀请家人共同关注用药情况。</text>
    </view>

    <view class="form-card">
      <view class="input-item">
        <text class="label">家庭组名称</text>
        <input 
          class="input-box" 
          v-model="formData.groupName" 
          placeholder="例如: 幸福一家人" 
          maxlength="64" 
        />
      </view>

      <view class="input-item">
        <text class="label">描述 (可选)</text>
        <textarea 
          class="textarea-box" 
          v-model="formData.description" 
          placeholder="输入关于这个家庭组的介绍..." 
          maxlength="256" 
        />
      </view>
    </view>

    <view class="action-section">
      <button 
        class="create-btn" 
        :loading="submitting" 
        :disabled="!formData.groupName"
        @click="handleCreate"
      >
        立即创建并成为组长
      </button>
      <text class="agreement-tips">创建后您将自动成为该组组长，拥有管理权限。</text>
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
        console.error("报错显示：",e)
        uni.showToast({ title: '网络请求异常', icon: 'none' });
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.create-container {
  padding: 60rpx 40rpx;
  background-color: #fcfcfc;
  min-height: 100vh;
}

.header-section {
  margin-bottom: 60rpx;
}

.main-title {
  font-size: 52rpx;
  font-weight: 700;
  color: #1a1a1a;
  display: block;
}

.tips {
  font-size: 28rpx;
  color: #888;
  margin-top: 20rpx;
  line-height: 1.5;
}

.form-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 40rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.05);
}

.input-item {
  margin-bottom: 40rpx;
}

.label {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
  display: block;
}

.input-box {
  height: 90rpx;
  border-bottom: 2rpx solid #eeeeee;
  font-size: 32rpx;
}

.textarea-box {
  width: 100%;
  height: 200rpx;
  background-color: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.action-section {
  margin-top: 80rpx;
  text-align: center;
}

.create-btn {
  background: linear-gradient(135deg, #007AFF, #005BBF);
  color: #ffffff;
  height: 100rpx;
  line-height: 100rpx;
  border-radius: 50rpx;
  font-size: 34rpx;
  font-weight: 600;
  box-shadow: 0 10rpx 20rpx rgba(0, 122, 255, 0.3);
}

.create-btn[disabled] {
  background: #ccc;
  box-shadow: none;
}

.agreement-tips {
  font-size: 24rpx;
  color: #aaa;
  margin-top: 30rpx;
  display: block;
}
</style>