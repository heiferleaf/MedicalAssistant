<!-- <template>
    <view class="container">
        <view class="header">
            <text class="title">自定义相机插件测试</text>
            <text class="subtitle">Android Studio 离线调试版</text>
        </view>

        <button class="btn" type="primary" @click="startRecognize">启动拍照识别</button>

        <scroll-view class="result-box" scroll-y="true" v-if="scanResult">
            <text class="result-title">算法返回结果：</text>
            <text class="result-content">{{ scanResult }}</text>
        </scroll-view>
    </view>
</template>

<script>
// 引入我们在 Android 端注册的原生插件
// ⚠️ 注意：这里的 'ScanCameraPlugin' 必须与你在 AS 离线工程 dcloud_uniplugins.json 中配置的 name 完全一致！
const ScanCameraPlugin = uni.requireNativePlugin('ScanCameraPlugin');

export default {
    data() {
        return {
            scanResult: ''
        }
    },
    methods: {
        startRecognize() {
            // 安全校验：防止在 H5 或标准基座下直接报错
            if (!ScanCameraPlugin) {
                uni.showToast({
                    title: '插件未加载，请确认已在 AS 中正确注册',
                    icon: 'none'
                });
                return;
            }

            // 每次调用前清空上一次的结果
            this.scanResult = '等待识别...';

            // 调用 Android 端 ScanModule.java 中用 @UniJSMethod 暴露的 startScan 方法
            ScanCameraPlugin.startScan((res) => {
                console.log("📥 收到原生层返回的数据：", res);

                if (res && res.code === 200) {
                    this.scanResult = res.data;
                    uni.showToast({
                        title: '识别完成',
                        icon: 'success'
                    });
                } else {
                    this.scanResult = '识别失败或未拿到数据';
                    uni.showToast({
                        title: '识别异常',
                        icon: 'error'
                    });
                }
            });
        }
    }
}
</script>

<style scoped>
.container {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 60rpx 40rpx;
    height: 100vh;
    box-sizing: border-box;
    background-color: #f8f9fa;
}

.header {
    text-align: center;
    margin-bottom: 80rpx;
}

.title {
    font-size: 44rpx;
    font-weight: bold;
    color: #333;
    display: block;
    margin-bottom: 10rpx;
}

.subtitle {
    font-size: 28rpx;
    color: #888;
}

.btn {
    width: 80%;
    height: 90rpx;
    line-height: 90rpx;
    border-radius: 45rpx;
    font-size: 32rpx;
    margin-bottom: 60rpx;
    box-shadow: 0 4rpx 12rpx rgba(0, 122, 255, 0.3);
}

.result-box {
    width: 100%;
    flex: 1;
    background-color: #282c34;
    border-radius: 16rpx;
    padding: 30rpx;
    box-sizing: border-box;
}

.result-title {
    font-size: 28rpx;
    color: #abb2bf;
    display: block;
    margin-bottom: 20rpx;
    font-weight: bold;
}

.result-content {
    font-size: 32rpx;
    color: #98c379;
    font-family: monospace;
    line-height: 1.5;
    word-break: break-all;
}
</style> -->

<template>
  <view>
    test camera
    test camera
    test camera
  </view>
</template>

<script>
export default {
    data() {
        return {
            // 这里可以定义一些数据属性
        }
    },
    methods: {
        // 这里可以定义一些方法
    }
}
</script>

<style>

</style>