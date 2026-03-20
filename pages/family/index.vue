<template>
    <view class="page-container">
        <view class="header">
            <view class="back-btn" @click="uni.navigateBack()">
                <image class="icon-sm" src="/static/Register/back.png" mode="aspectFit"></image>
            </view>
            <text class="header-title">欢迎</text>
            <view class="placeholder"></view>
        </view>

        <scroll-view scroll-y class="main-content">

            <view class="hero-section">
                <view class="icon-wrapper">
                    <image class="icon-xl" src="/static/family/family_restroom.svg" mode="aspectFit"></image>
                </view>
                <text class="main-title">欢迎来到家庭健康组</text>
                <text class="sub-title">与您的家人联系，共同管理健康。创建一个新的家庭组或加入现有家庭组。</text>
            </view>

            <view class="action-section">
                <button class="btn-primary" hover-class="btn-primary-hover" @click="navigateTo('create')">
                    <image class="icon-md mr-2" src="/static/Health/plus-circle.svg" mode="aspectFit"></image>
                    <text>创建家庭组</text>
                </button>
                <button class="btn-outline" hover-class="btn-outline-hover" @click="navigateTo('join')">
                    <image class="icon-md mr-2" src="/static/family/group_add.svg" mode="aspectFit"></image>
                    <text>加入家庭组</text>
                </button>
            </view>

            <view class="invite-section">
                <view class="section-header" v-if="inviteList.length > 0">
                    <text class="section-title">收到的邀请</text>
                    <view class="badge"><text class="badge-text">{{ inviteList.length }} 新</text></view>
                </view>

                <view class="invite-card" v-for="invite in inviteList" :key="invite.id">
                    <view class="card-top">
                        <view class="avatar">
                            <image class="avatar-img" src="/static/avatars/avatar2.svg" mode="aspectFill"></image>
                        </view>
                        <view class="invite-info">
                            <text class="info-label">邀请来自</text>
                            <text class="inviter-name">{{ invite.inviterNickname }}</text>
                            <text class="invite-quote">{{ invite.remark }}</text>
                        </view>
                    </view>

                    <view class="card-actions">
                        <button class="btn-reject" hover-class="btn-reject-hover" @click="handleInvite(invite, 'reject')">拒绝</button>
                        <button class="btn-accept" hover-class="btn-accept-hover" @click="handleInvite(invite, 'accept')">同意</button>
                    </view>
                </view>
            </view>

            <view class="bottom-spacer"></view>
        </scroll-view>
    </view>
</template>

<script>
import familyApi from '../../api/family';

export default {
    data() {
        return {
            userStatus: null, // 1: 组长, 0: 组员, null: 无家庭组
            loading: true,
            inviteList: [] // 存储待处理邀请
        };
    },
    onShow() {
        this.checkUserFamilyStatus();
    },
    methods: {
        async checkUserFamilyStatus() {
            this.loading = true;
            try {
                // 1. 获取当前组状态
                let res = await familyApi.getMyGroup();

                if (res && res.group) {
                    // 根据逻辑判断身份 (此处沿用你的逻辑，有 ownerNickname 则视为组长)
                    this.userStatus = res.group.ownerUserNickname ? 1 : 0;
                    this.autoNavigate();
                } else {
                    this.userStatus = null;
                    // 2. 只有在没有家庭组时，才获取邀请列表
                    await this.fetchInvites();
                }
            } catch (e) {
                console.error("获取状态失败", e);
                await this.fetchInvites();
            } finally {
                this.loading = false;
            }
        },

        async fetchInvites() {
            try {
                const inviteRes = await familyApi.getMyApplyRecords();
                // 过滤出 pending 状态的记录
                this.inviteList = (inviteRes || []).filter(item => item.status === 'pending' && item.type === 'invite');
                console.log("待处理邀请列表：", JSON.stringify(this.inviteList));
            } catch (e) {
                console.error("获取邀请列表失败", e);
            }
        },

        /**
         * 处理邀请：同意或拒绝
         * @param {Object} invite 邀请条目
         * @param {String} type 'approve' 或 'reject'
         */
        async handleInvite(invite, type) {
            uni.showLoading({ title: '处理中...' });
            console.log("邀请ID:", JSON.stringify(invite));
            try {
                // opType 根据后端定义，通常 approve 为同意，reject 为拒绝
                // const applyId = uni.getStorageSync("userId");
                await familyApi.approveApply(invite.groupId, {
                    applyId: invite.applyId,
                    opType: type
                });

                uni.showToast({ title: type === 'approve' ? '已加入家庭' : '已拒绝' });

                // 处理完成后重新刷新整体状态
                this.checkUserFamilyStatus();
            } catch (e) {
                console.error(e);
                uni.showToast({ title: '操作失败', icon: 'none' });
            } finally {
                uni.hideLoading();
            }
        },

        autoNavigate() {
            if (this.userStatus !== null) {
                uni.reLaunch({ url: '/pages/family/members' });
            }
        },

        navigateTo(type) {
            const urlMap = {
                'create': '/pages/family/create',
                'join': '/pages/family/join'
            };
            uni.navigateTo({ url: urlMap[type] });
        }
    }
};
</script>
<style lang="scss" scoped>
/* 全局变量 */
$primary: #4d88ff;
$bg-color: #f5f6f8;
$card-bg: #ffffff;
$text-main: #0f172a;
$text-sub: #64748b;
$border-color: #e2e8f0;

/* 图标尺寸工具 */
.icon-sm {
    width: 40rpx;
    height: 40rpx;
    flex-shrink: 0;
}

.icon-md {
    width: 48rpx;
    height: 48rpx;
    flex-shrink: 0;
}

.icon-xl {
    width: 80rpx;
    height: 80rpx;
    flex-shrink: 0;
}

.icon-tab {
    width: 48rpx;
    height: 48rpx;
    flex-shrink: 0;
    margin-bottom: 8rpx;
}

.mr-2 {
    margin-right: 16rpx;
}

.page-container {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: $bg-color;
    font-family: 'Inter', sans-serif;
}

/* 顶部导航 */
.header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20rpx 32rpx;
    background-color: $card-bg;
    border-bottom: 2rpx solid $border-color;
    position: sticky;
    top: 0;
    z-index: 10;

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

    .header-title {
        font-size: 36rpx;
        font-weight: bold;
        color: $text-main;
        flex: 1;
        text-align: center;
    }

    .placeholder {
        width: 80rpx;
    }
}

/* 主内容区 */
.main-content {
    flex: 1;
    height: 0;
}

/* 头部欢迎区 */
.hero-section {
    padding: 80rpx 48rpx 64rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .icon-wrapper {
        width: 160rpx;
        height: 160rpx;
        background-color: rgba($primary, 0.1);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 48rpx;
    }

    .main-title {
        font-size: 56rpx;
        font-weight: bold;
        color: $text-main;
        margin-bottom: 24rpx;
    }

    .sub-title {
        font-size: 32rpx;
        color: $text-sub;
        line-height: 1.5;
        max-width: 600rpx;
    }
}

/* 操作按钮区 */
.action-section {
    padding: 0 48rpx;
    display: flex;
    flex-direction: column;
    gap: 24rpx;

    .btn-primary,
    .btn-outline {
        width: 100%;
        height: 112rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 24rpx;
        font-size: 32rpx;
        font-weight: bold;

        &::after {
            border: none;
        }
    }

    .btn-primary {
        background-color: $primary;
        color: #ffffff;
        box-shadow: 0 8rpx 24rpx rgba($primary, 0.2);
    }

    .btn-primary-hover {
        background-color: #3b76eb;
        transform: scale(0.99);
    }

    .btn-outline {
        background-color: $card-bg;
        color: $text-main;
        border: 4rpx solid $border-color;
    }

    .btn-outline-hover {
        border-color: rgba($primary, 0.5);
        background-color: #f8fafc;
    }
}

/* 邀请通知区 */
.invite-section {
    padding: 96rpx 48rpx 48rpx;

    .section-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 32rpx;

        .section-title {
            font-size: 36rpx;
            font-weight: bold;
            color: $text-main;
        }

        .badge {
            background-color: rgba($primary, 0.1);
            padding: 8rpx 16rpx;
            border-radius: 999rpx;

            .badge-text {
                color: $primary;
                font-size: 24rpx;
                font-weight: bold;
            }
        }
    }

    .invite-card {
        background-color: $card-bg;
        border-radius: 32rpx;
        padding: 40rpx;
        border: 2rpx solid $border-color;
        box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.02);

        .card-top {
            display: flex;
            gap: 32rpx;
            margin-bottom: 40rpx;

            .avatar {
                width: 96rpx;
                height: 96rpx;
                border-radius: 50%;
                background-color: #f1f5f9;
                overflow: hidden;
                flex-shrink: 0;

                .avatar-img {
                    width: 100%;
                    height: 100%;
                }
            }

            .invite-info {
                flex: 1;
                display: flex;
                flex-direction: column;

                .info-label {
                    font-size: 28rpx;
                    color: $text-sub;
                }

                .inviter-name {
                    font-size: 32rpx;
                    font-weight: bold;
                    color: $text-main;
                    margin-top: 4rpx;
                }

                .invite-quote {
                    font-size: 24rpx;
                    color: #94a3b8;
                    font-style: italic;
                    margin-top: 8rpx;
                    line-height: 1.4;
                }
            }
        }

        .card-actions {
            display: flex;
            gap: 24rpx;

            .btn-reject,
            .btn-accept {
                flex: 1;
                height: 88rpx;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 16rpx;
                font-size: 30rpx;
                font-weight: 600;

                &::after {
                    border: none;
                }
            }

            .btn-reject {
                background-color: #f1f5f9;
                color: #475569;
            }

            .btn-reject-hover {
                background-color: #fee2e2;
                color: #dc2626;
            }

            .btn-accept {
                background-color: $primary;
                color: #ffffff;
            }

            .btn-accept-hover {
                box-shadow: 0 4rpx 12rpx rgba($primary, 0.3);
                opacity: 0.9;
            }
        }
    }
}

.bottom-spacer {
    height: 160rpx;
}

/* 底部导航栏 */
.custom-tab-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    display: flex;
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(20px);
    border-top: 2rpx solid $border-color;
    padding: 16rpx 32rpx 8rpx;
    z-index: 20;

    &.pb-safe {
        padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
    }

    .tab-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .tab-text {
            font-size: 20rpx;
            font-weight: bold;
            text-transform: uppercase;
            letter-spacing: 2rpx;
            color: $text-sub;
            margin-top: 4rpx;
        }

        &.active {
            .tab-text {
                color: $primary;
            }
        }
    }
}
</style>