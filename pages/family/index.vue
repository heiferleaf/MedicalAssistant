<template>
    <view class="container">
        <view v-if="loading" class="loading-status">
            <text>正在获取家庭组状态...</text>
        </view>

        <view v-else>
            <view v-if="userStatus === null" class="no-group-section">
                <view class="welcome-card">
                    <text class="title">欢迎使用健康家庭组</text>
                    <text class="subtitle">您可以创建一个新家庭组或加入现有小组</text>
                </view>

                <button class="btn primary-btn" @click="navigateTo('create')">创建家庭组</button>
                <button class="btn secondary-btn" @click="navigateTo('join')">申请加入家庭组</button>
            </view>

            <view v-else class="in-group-section">
                <view class="status-card">
                    <text class="role-tag">{{ userStatus === 1 ? '家庭组长' : '家庭成员' }}</text>
                    <text class="info">您已加入家庭组，正在为您跳转管理页面...</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import familyApi from '../../api/family';
export default {
    data() {
        return {
            userStatus: null, // 1: 组长, 0: 组员, null: 无家庭组
            loading: true
        };
    },
    onShow() {
        // 需求要求所有数据实时获取，不留本地缓存
        this.checkUserFamilyStatus();
    },
    methods: {
        /**
         * 获取用户当前的家庭组角色状态
         */
        async checkUserFamilyStatus() {
            this.loading = true;
            try {
                // 根据 role 字段判断：'leader' 为 1，'normal' 为 0
                let res = await familyApi.getMyGroup();
                if (res == null)this.userStatus = null;
                else if (res.group.ownerUserNickname)this.userStatus = 1;
                else this.userStatus = 0;
                this.autoNavigate();
            } catch (e) {
                console.error("获取状态失败", e);
            } finally {
                this.loading = false;
            }
        },

        /**
         * 自动跳转逻辑：已入组用户直接进入成员信息页
         */
        autoNavigate() {
            if (this.userStatus !== null) {
                uni.reLaunch({
                    url: '/pages/family/members'
                });
            }
        },

        /**
         * 手动跳转逻辑：无组用户选择创建或申请
         */
        navigateTo(type) {
            const urlMap = {
                'create': '/pages/family/create',
                'join': '/pages/family/join'
            };
            uni.navigateTo({
                url: urlMap[type]
            });
        }
    }
};
</script>

<style scoped>
.container {
    padding: 40rpx;
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-height: 80vh;
}

.welcome-card,
.status-card {
    text-align: center;
    margin-bottom: 60rpx;
}

.title {
    font-size: 48rpx;
    font-weight: bold;
    display: block;
    margin-bottom: 20rpx;
}

.subtitle {
    font-size: 28rpx;
    color: #666;
}

.btn {
    margin-top: 30rpx;
    width: 100%;
    border-radius: 12rpx;
}

.primary-btn {
    background-color: #007AFF;
    color: white;
}

.secondary-btn {
    background-color: #f8f8f8;
    color: #333;
    border: 1px solid #ddd;
}

.role-tag {
    background: #e1f5fe;
    color: #01579b;
    padding: 10rpx 30rpx;
    border-radius: 40rpx;
    font-size: 24rpx;
}
</style>