<template>
    <view class="container" :class="{ 'dark-mode': isDarkMode }">
        <view class="settings-group">
            <view class="group-title">显示与外观</view>
            <view class="card">
                <view class="setting-item">
                    <view class="item-left">
                        <text class="item-title" :style="{ fontSize: fontSize + 'px' }">深色模式</text>
                        <text class="item-desc">开启后，界面将适应暗光环境</text>
                    </view>
                    <switch :checked="isDarkMode" @change="onThemeChange" color="#6366f1" />
                </view>

                <view class="divider"></view>

                <view class="setting-item font-item">
                    <view class="item-left">
                        <text class="item-title" :style="{ fontSize: fontSize + 'px' }">系统字体大小</text>
                        <text class="item-desc">当前: {{ fontSize }}px</text>
                    </view>
                    <slider 
                        class="font-slider" 
                        :value="fontSize" 
                        min="12" 
                        max="24" 
                        step="2" 
                        activeColor="#6366f1" 
                        backgroundColor="#e2e8f0" 
                        @change="onFontChange" 
                        @changing="onFontChanging"
                    />
                </view>
            </view>
        </view>

        <view class="settings-group">
            <view class="group-title">关于</view>
            <view class="card">
                <view class="setting-item" @tap="checkUpdate">
                    <text class="item-title" :style="{ fontSize: fontSize + 'px' }">当前版本</text>
                    <view class="item-right">
                        <text class="version-text">{{ appVersion }}</text>
                        <text class="arrow">></text>
                    </view>
                </view>
            </view>
        </view>

        <view class="footer-tips">
            <text>Health & Medication Agent 提供技术支持</text>
        </view>
    </view>
</template>

<script>
export default {
    data() {
        return {
            isDarkMode: false,
            fontSize: 16, // 默认字体大小 16px
            appVersion: "v1.0.0"
        };
    },
    onLoad() {
        this.initSettings();
    },
    methods: {
        // 1. 初始化读取本地缓存和系统信息
        initSettings() {
            // 读取主题设置
            const savedTheme = uni.getStorageSync('app_theme');
            if (savedTheme === 'dark') {
                this.isDarkMode = true;
            }

            // 读取字体设置
            const savedFont = uni.getStorageSync('app_font_size');
            if (savedFont) {
                this.fontSize = savedFont;
            }

            // 获取真实的 App 版本号 (如果是 App 端)
            // const sysInfo = uni.getSystemInfoSync();
            // if (sysInfo.appVersion) {
            //     this.appVersion = `v${sysInfo.appVersion}`;
            // }
        },

        // 2. 切换深色模式
        onThemeChange(e) {
            this.isDarkMode = e.detail.value;
            const themeStr = this.isDarkMode ? 'dark' : 'light';
            uni.setStorageSync('app_theme', themeStr);
            
            // 提示用户
            uni.showToast({
                title: '主题已切换，部分页面需重启生效',
                icon: 'none'
            });

            // ⚠️ 注意：如果要全局生效，这里需要配合全局状态管理 (Vuex/Pinia) 
            // 或使用 uniapp 官方的 theme.json 配置
        },

        // 3. 拖动滑动条时实时预览大小
        onFontChanging(e) {
            this.fontSize = e.detail.value;
        },

        // 4. 松开滑动条确定修改字体
        onFontChange(e) {
            this.fontSize = e.detail.value;
            uni.setStorageSync('app_font_size', this.fontSize);
            
            uni.showToast({
                title: '字体大小已保存',
                icon: 'success'
            });
        },

        // 5. 检查更新（模拟）
        checkUpdate() {
            uni.showLoading({ title: '检查更新中...' });
            setTimeout(() => {
                uni.hideLoading();
                uni.showToast({
                    title: '当前已是最新版本',
                    icon: 'none'
                });
            }, 1000);
        }
    }
};
</script>

<style lang="scss" scoped>
/* 页面底层背景 */
.container {
    min-height: 100vh;
    background-color: #f8fafc;
    padding: 30rpx;
    transition: background-color 0.3s ease;

    /* 暗黑模式底层背景 */
    &.dark-mode {
        background-color: #0f172a;

        .group-title {
            color: #94a3b8;
        }

        .card {
            background-color: #1e293b;
            box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
            
            .item-title {
                color: #f1f5f9;
            }
            .item-desc, .version-text, .arrow {
                color: #64748b;
            }
        }
        
        .divider {
            background-color: #334155;
        }
    }
}

.settings-group {
    margin-bottom: 40rpx;

    .group-title {
        font-size: 26rpx;
        color: #64748b;
        margin-bottom: 20rpx;
        padding-left: 10rpx;
    }
}

.card {
    background-color: #ffffff;
    border-radius: 24rpx;
    padding: 10rpx 30rpx;
    box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.03);
    transition: background-color 0.3s ease;
}

.setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 0;
    min-height: 80rpx;

    &.font-item {
        flex-direction: column;
        align-items: flex-start;
        
        .item-left {
            width: 100%;
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20rpx;
        }

        .font-slider {
            width: 100%;
            margin: 0;
        }
    }

    .item-left {
        display: flex;
        flex-direction: column;
        justify-content: center;
    }

    .item-title {
        color: #334155;
        font-weight: 500;
        transition: color 0.3s ease, font-size 0.2s ease;
    }

    .item-desc {
        font-size: 24rpx;
        color: #94a3b8;
        margin-top: 8rpx;
    }

    .item-right {
        display: flex;
        align-items: center;

        .version-text {
            color: #64748b;
            font-size: 28rpx;
            margin-right: 10rpx;
        }

        .arrow {
            color: #cbd5e1;
            font-size: 32rpx;
        }
    }
}

.divider {
    height: 1rpx;
    background-color: #f1f5f9;
    width: 100%;
    transition: background-color 0.3s ease;
}

.footer-tips {
    text-align: center;
    margin-top: 60rpx;
    
    text {
        font-size: 22rpx;
        color: #cbd5e1;
    }
}
</style>