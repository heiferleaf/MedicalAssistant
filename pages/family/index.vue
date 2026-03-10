<template>
    <view class="container">
        <view v-if="loading" class="loading-status">
            <text>正在获取家庭组状态...</text>
        </view>

        <view v-else>
            <view v-if="userStatus !== null" class="in-group-section">
                <view class="status-card">
                    <text class="role-tag">{{ userStatus === 1 ? '家庭组长' : '家庭成员' }}</text>
                    <text class="info">您已加入家庭组，正在为您跳转管理页面...</text>
                </view>
            </view>

            <view v-else class="no-group-section">
                <view class="welcome-card">
                    <text class="title">欢迎使用健康家庭组</text>
                    <text class="subtitle">您可以创建一个新家庭组或加入现有小组</text>
                </view>

                <button class="btn primary-btn" @click="navigateTo('create')">创建家庭组</button>
                <button class="btn secondary-btn" @click="navigateTo('join')">申请加入家庭组</button>

                <view v-if="inviteList.length > 0" class="invite-section">
                    <view class="section-title">收到的邀请</view>
                    <view v-for="item in inviteList" :key="item.inviteId" class="invite-item">
                        <view class="invite-info">
                            <text class="group-name">{{ item.groupName }}</text>
                            <text class="inviter">邀请人：{{ item.inviterNickname }}</text>
                            <text class="remark" v-if="item.remark">"{{ item.remark }}"</text>
                        </view>
                        <view class="invite-ops">
                            <button class="op-btn reject" @click="handleInvite(item, 'reject')">拒绝</button>
                            <button class="op-btn approve" @click="handleInvite(item, 'accept')">同意</button>
                        </view>
                    </view>
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
            console.log("邀请ID:",JSON.stringify(invite));
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

<style scoped>
.container {
    padding: 40rpx;
    background-color: #fcfcfc;
    min-height: 100vh;
}

/* 欢迎卡片样式保持 */
.welcome-card { text-align: center; margin: 60rpx 0; }
.title { font-size: 44rpx; font-weight: bold; margin-bottom: 16rpx; display: block; }
.subtitle { font-size: 26rpx; color: #888; }

/* 按钮样式 */
.btn { margin-top: 24rpx; font-size: 30rpx; height: 90rpx; line-height: 90rpx; }
.primary-btn { background-color: #007AFF; color: white; }
.secondary-btn { background-color: #fff; color: #333; border: 1px solid #eee; }

/* 申请记录样式 */
.apply-history {
    margin-top: 80rpx;
}
.section-title {
    font-size: 32rpx;
    font-weight: bold;
    margin-bottom: 20rpx;
    color: #333;
}
.record-card {
    background: #fff;
    padding: 30rpx;
    border-radius: 16rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}
.record-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16rpx;
}
.group-name { font-size: 30rpx; font-weight: 500; }
.status-tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; }
.status-tag.pending { background: #fff8e1; color: #f57f17; }
.status-tag.rejected { background: #ffebee; color: #c62828; }

.record-body .detail { font-size: 24rpx; color: #666; display: block; margin-top: 4rpx; }
.record-body .time { font-size: 22rpx; color: #999; display: block; margin-top: 10rpx; }

.role-tag { background: #e1f5fe; color: #01579b; padding: 10rpx 30rpx; border-radius: 40rpx; font-size: 24rpx; }
</style>