<template>
  <view class="container">
    <view class="bg-gradient"></view>

    <view class="header">
      <view style="display: flex;width: 100%;">
        <view class="back-btn" @click="uni.navigateBack()">
          <image
            class="icon-sm"
            src="/static/Register/back.png"
            mode="aspectFit"
          ></image>
        </view>
        <text class="title">智能拍照识别</text></view
      >
      <view class="subtitle-box">
        <text class="dot">●</text>
        <text class="subtitle">由 AI 提供医药数字化识别支持</text>
      </view>
    </view>

    <view class="preview-card">
      <image
        v-if="tempThumb"
        :src="tempThumb"
        mode="aspectFit"
        class="main-img"
      ></image>

      <view v-else class="guide-box" @tap="handleCapture">
        <view class="scan-frame">
          <view class="frame-line tl"></view>
          <view class="frame-line tr"></view>
          <view class="frame-line bl"></view>
          <view class="frame-line br"></view>
          <image
            class="center-icon"
            src="/static/DrugScan/camera-blue.svg"
            style="width: 80rpx; height: 80rpx"
          />
        </view>

        <view class="tips-text">
          <text class="main-tip">点击拍摄药品包装</text>
          <text class="sub-tip">请确保文字清晰、无反光遮挡</text>
        </view>

        <view class="notice-tags">
          <view class="tag">√ 识别用法</view>
          <view class="tag">√ 校验剂量</view>
          <view class="tag">√ 禁忌提醒</view>
        </view>
      </view>
    </view>

    <view class="footer">
      <view class="safety-hint">
        <image
          src="/static/Health/shield.svg"
          style="width: 24rpx; height: 24rpx; margin-right: 8rpx"
        />
        <text>识别结果仅供参考，请遵循医嘱</text>
      </view>
      <button class="btn-primary" style="width:80%;" @tap="handleCapture" v-if="!tempThumb">
        <view class="ai-sparkle"></view>
        <text class="btn-text">开始智能识别</text>
      </button>
      <view v-else class="button-group">
        <button class="btn-primary" @tap="handleCancel">
          <text class="btn-text">取消</text>
        </button>

        <button class="btn-primary" @tap="handleCapture">
          <text class="btn-text">重拍</text>
        </button>

        <button class="btn-ai-special" @tap="sendToAI">
          <view class="shimmer"></view>
          <image
            class="ai-icon"
            src="/static/index/AI.png"
            style="width: 32rpx; height: 32rpx; margin-right: 8rpx"
          />
          <text class="btn-text">询问 AI 助手</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      tempThumb: "", // 用于页面展示的缩略图
      showMenu: false, // 弹窗控制
    };
  },
  methods: {
    // 1. 触发拍照（兼容模拟器和真机）
    handleCapture() {
      uni.chooseImage({
        count: 1,
        sizeType: ["compressed"], // 压缩图片，提升上传速度
        sourceType: ["camera"], // 只开放相机，如需相册可加入 'album'
        success: (res) => {
          this.tempThumb = res.tempFilePaths[0];
          this.showMenu = true; // 拍完弹出选项
        },
        fail: (err) => {
          console.log("用户取消或权限拒绝", err);
        },
      });
    },
	handleCancel() {
	  this.tempThumb = "";
	  this.showMenu = false;
	  uni.navigateTo({ url: "/pages/index/index" });
	},

    // 2. 发送给 AI
    async sendToAI() {
      this.showMenu = false;
      uni.showLoading({ title: "准备传输中..." });

      try {
        // 将图片存入全局缓存，方便 AI 页面读取
        uni.setStorageSync("last_scan_image", this.tempThumb);

        // 跳转到 AI 对话页
        uni.navigateTo({
          url: "/pages/ai/Assistant?from=scan",
          success: () => {
            uni.hideLoading();
          },
        });
      } catch (e) {
        uni.showToast({ title: "图片处理失败", icon: "none" });
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.container {
  height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 40rpx;
}

.bg-gradient {
  position: absolute;
  top: 0;
  width: 100%;
  height: 400rpx;
  background: linear-gradient(180deg, #3b82f6 0%, rgba(59, 130, 246, 0) 100%);
  z-index: 1;
}

.header {
	width:100%;
  margin-top: 120rpx;
  text-align: center;
  z-index: 2;
  .title {
    font-size: 48rpx;
    font-weight: bold;
    color: #fff;
    display: block;
	margin-left: 15%;
	margin-right: 20%;
	text-align: center;
  }
  .subtitle {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.8);
    margin-top: 10rpx;
  }
  .back-btn {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;

    &:active {
      background-color: #f1f5f9;
    }
  }
}

.icon-sm {
  width: 40rpx;
  height: 40rpx;
  flex-shrink: 0;
}

.preview-card {
  width: 100%;
  height: 600rpx;
  background: #fff;
  border-radius: 32rpx;
  margin-top: 60rpx;
  box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.05);
  z-index: 2;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;

  .main-img {
    width: 100%;
    height: 100%;
  }
  .placeholder {
    text-align: center;
    color: #94a3b8;
    .plus-icon {
      font-size: 80rpx;
      margin-bottom: 20rpx;
    }
  }
}

.footer {
  margin-top: auto;
  margin-bottom: 100rpx;
  width: 100%;
  z-index: 2;
  box-sizing: border-box;
  padding-left: 20rpx;
  padding-right: 20rpx;
  .btn-primary {
    background: #3b82f6;
    width: 25%;
    height: 100rpx;
    border-radius: 30rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 10rpx 20rpx rgba(59, 130, 246, 0.3);
    .btn-text {
      color: #fff;
      font-weight: bold;
    }
  }
}

/* 弹窗样式 */
.mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 998;
}

.action-sheet {
  position: fixed;
  bottom: -600rpx;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx;
  transition: all 0.3s ease;
  z-index: 999;

  &.sheet-show {
    bottom: 0;
  }

  .sheet-title {
    font-size: 24rpx;
    color: #94a3b8;
    text-align: center;
    margin-bottom: 30rpx;
  }
  .sheet-item {
    height: 110rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32rpx;
    border-bottom: 1rpx solid #f1f5f9;
    &:active {
      background: #f8fafc;
    }
    &.ai-btn {
      color: #3b82f6;
      font-weight: bold;
    }
    &.cancel {
      border: none;
      color: #ef4444;
      margin-top: 10rpx;
    }
  }
}

/* 在原有样式基础上添加/修改 */

.header {
  margin-top: 100rpx;
  .title {
    font-size: 52rpx;
    letter-spacing: 2rpx;
  }
  .subtitle-box {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 16rpx;
    .dot {
      color: #10b981;
      font-size: 18rpx;
      margin-right: 12rpx;
    }
    .subtitle {
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.7);
    }
  }
}

.guide-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;

  .scan-frame {
    width: 200rpx;
    height: 200rpx;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 40rpx;

    .frame-line {
      position: absolute;
      width: 40rpx;
      height: 40rpx;
      border: 4rpx solid #3b82f6;
      &.tl {
        top: 0;
        left: 0;
        border-right: none;
        border-bottom: none;
        border-radius: 12rpx 0 0 0;
      }
      &.tr {
        top: 0;
        right: 0;
        border-left: none;
        border-bottom: none;
        border-radius: 0 12rpx 0 0;
      }
      &.bl {
        bottom: 0;
        left: 0;
        border-right: none;
        border-top: none;
        border-radius: 0 0 0 12rpx;
      }
      &.br {
        bottom: 0;
        right: 0;
        border-left: none;
        border-top: none;
        border-radius: 0 0 12rpx 0;
      }
    }
  }

  .tips-text {
    text-align: center;
    .main-tip {
      color: #1e293b;
      font-size: 34rpx;
      font-weight: 600;
      display: block;
    }
    .sub-tip {
      color: #94a3b8;
      font-size: 24rpx;
      margin-top: 12rpx;
      display: block;
    }
  }

  .notice-tags {
    display: flex;
    gap: 20rpx;
    margin-top: 60rpx;
    .tag {
      background: #f1f5f9;
      color: #64748b;
      font-size: 20rpx;
      padding: 8rpx 20rpx;
      border-radius: 100rpx;
    }
  }
}

.safety-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.btn-primary {
  /* 增加一个流光渐变 */
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  position: relative;
  overflow: hidden;

  &:active {
    opacity: 0.9;
    transform: scale(0.98);
  }
}

.button-group {
  display: flex;
  gap: 20rpx;
  width: 100%;
  padding: 0 30rpx;
  justify-content: space-between;
}

/* 次要按钮：简约灰/白 */
.btn-secondary {
  flex: 1;
  height: 100rpx;
  border-radius: 24rpx;
  background: #f1f5f9;
  border: 1rpx solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;

  .btn-text {
    color: #64748b;
    font-size: 28rpx;
    font-weight: 500;
  }

  &:active {
    background: #e2e8f0;
    transform: scale(0.96);
  }
}

/* AI 特别按钮：紫色流光 */
.btn-ai-special {
  flex: 1; /* 宽度占比更大，视觉重心 */
  height: 100rpx;
  border-radius: 24rpx;
  width: 40%;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden; /* 必须，否则流光会溢出 */
  box-shadow: 0 10rpx 30rpx rgba(168, 85, 247, 0.3);
  margin: 0;
  border: none;

  .btn-text {
    color: #ffffff;
    font-size: 30rpx;
    font-weight: 600;
    z-index: 2;
  }

  .ai-icon {
    z-index: 2;
    filter: brightness(0) invert(1); /* 图标变白 */
  }

  /* 核心：流光动画 */
  .shimmer {
    position: absolute;
    top: 0;
    left: -100%;
    width: 50%;
    height: 100%;
    background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.3),
      transparent
    );
    transform: skewX(-20deg);
    animation: shimmer-run 2.5s infinite;
    z-index: 1;
  }

  &:active {
    transform: scale(0.96);
    filter: brightness(1.1);
  }
}

@keyframes shimmer-run {
  0% {
    left: -100%;
  }
  50% {
    left: 150%;
  }
  100% {
    left: 150%;
  }
}
</style>