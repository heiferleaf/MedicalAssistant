<template>
    <view class="members-container">
        <view class="group-card">
            <view class="group-info">
                <text class="group-name">{{ groupInfo.groupName || '加载中...' }}</text>
                <text class="group-id">ID: {{ groupInfo.groupId }}</text>
            </view>
            <view class="role-badge" :class="userRole">
                {{ userRole === 'leader' ? '组长' : '成员' }}
            </view>
        </view>

        <view class="admin-section" v-if="userRole === 'leader'">
            <view class="admin-item" @click="navigateTo(`/pages/family/invite?groupId=${groupInfo.groupId}`)">
                <image class="icon" src="../../static/Health/plus-circle.svg" />
                <text>邀请成员</text>
            </view>
            <view class="admin-item" @click="openApplyCenter">
                <image class="icon" src="../../static/Home/bell.svg" />
                <text>审批中心</text>
                <view v-if="hasNewApply" class="dot"></view>
            </view>
        </view>

        <view class="list-title">
            <text>家庭成员 ({{ memberList.length }})</text>
            <text class="subtitle">点击查看详细健康数据</text>
        </view>

        <view class="member-list">
            <view class="member-item" v-for="item in memberList" :key="item.user_id" @click="goToHealthData(item)">
                <view class="avatar">{{ item.nickname ? item.nickname[0] : '👤' }}</view>
                <view class="member-detail">
                    <text class="name">{{ item.nickname }}</text>
                    <text class="role-text">{{ item.role === 'leader' ? '组长' : '成员' }}</text>
                </view>
                <view class="arrow">▶</view>
            </view>
        </view>

        <view class="footer-action">
            <button v-if="userRole === 'normal'" class="quit-btn" @click="handleQuit">退出家庭组</button>
            <text v-else class="leader-tips">组长如需退出请先解散或转让</text>
        </view>
        <view class="mask" v-if="showApplyModal" @click="showApplyModal = false">
            <view class="modal-content" @click.stop>
                <view class="modal-header">
                    <text class="modal-title">待处理申请</text>
                    <text class="close-btn" @click="showApplyModal = false">✕</text>
                </view>

                <scroll-view scroll-y class="modal-body">
                    <view v-if="applyList.length === 0" class="empty-apply">
                        暂无待处理的申请
                    </view>
                    <view v-for="apply in applyList" :key="apply.applyId" class="apply-item">
                        <view class="apply-info">
                            <view class="apply-user-box">
                                <text class="apply-user">{{ apply.inviterNickname || '未知用户' }}</text>
                                <text class="apply-type">{{ apply.type === 'invite' ? '邀请加入' : '申请加入' }}</text>
                            </view>
                            <text class="apply-remark">备注: {{ apply.remark }}</text>
                            <text class="apply-time">{{ apply.createTime }}</text>
                        </view>
                        <view class="apply-ops">
                            <button class="op-btn approve" @click="handleApply(apply, 'accept')">同意</button>
                            <button class="op-btn reject" @click="handleApply(apply, 'reject')">拒绝</button>
                        </view>
                    </view>
                </scroll-view>
            </view>
        </view>
    </view>
</template>

<script>
import familyApi from '../../api/family';
export default {
    data() {
        return {
            userRole: 'normal', // 'leader' 或 'normal' [cite: 44]
            groupInfo: {},
            memberList: [],
            hasNewApply: false,
            loading: false,
            showApplyModal: false, // 控制弹窗显示
            applyList: [],        // 申请列表数据
        };
    },
    onLoad() {
        // 1. 页面进入时先加载一次初始数据
        this.initData();

        // 2. 开启监听全局刷新事件
        uni.$on("REFRESH_MEMBER_LIST", (data) => {
            console.log("收到成员变动通知，开始局部刷新:", data);

            // 方案 A：直接重新调用接口获取最新列表（最稳妥，防止本地逻辑漏算）
            this.initData();
        });
    },
    onUnload() {
        // 3. 页面销毁时一定要移除监听，否则会造成内存泄漏和重复触发
        uni.$off("REFRESH_MEMBER_LIST");
    },
    methods: {
        async initData() {
            await this.fetchData();
            await this.getMessage();
        },

        // 点击“审批中心”时触发
        openApplyCenter() {
            if (this.applyList.length === 0) {
                uni.showToast({ title: '暂无待处理申请', icon: 'none' });
                return;
            }
            this.showApplyModal = true;
        },

        // 处理申请（同意/拒绝）
        async handleApply(apply, action) {
            uni.showLoading({ title: '处理中...' });
            try {
                // 假设你的 api 有这个方法，参数为 applyId 和 动作
                let params = {
                    opType: action,
                    remark: "",
                    applyId: apply.applyId
                }
                const res = await familyApi.approveApply(this.groupInfo.groupId, params);

                if (res.code === 200) {
                    uni.showToast({ title: '处理成功' });
                    this.getMessage(); // 刷新列表
                    this.fetchData(); // 刷新成员列表（如果同意了人的话）
                    if (this.applyList.length <= 1) this.showApplyModal = false;
                }
            } catch (e) {
                console.error(e);
                uni.showToast({ title: '操作失败', icon: 'none' });
            } finally {
                uni.hideLoading();
            }
        },
        async getMessage() {
            try {
                let res = await familyApi.getPendingApplies(this.groupInfo.groupId);

                // 只要 res 存在、是数组且长度大于 0，就设为 true
                if (res && Array.isArray(res) && res.length > 0) {
                    this.hasNewApply = true;
                    this.applyList = (res || []).filter(item => item.status === 'pending');
                    this.hasNewApply = this.applyList.length > 0;
                    console.log("检测到新申请: ", res[0]);
                } else {
                    this.hasNewApply = false;
                }

            } catch (e) {
                console.error("获取申请记录失败：", e);
            }
        },
        async fetchData() {
            // 每次进入页面实时获取最新成员列表，不留缓存 
            try {
                let res = await familyApi.getMyGroup();
                this.groupInfo = res.group;
                this.memberList = res.members;
                // 1. 同步获取缓存中的用户名
                const currentUserName = uni.getStorageSync('userName');
                console.log("家庭组成员:", res.members, "当前用户名:", currentUserName);

                // 2. 从 res.members 数组中查找匹配的成员
                if (res.members && Array.isArray(res.members)) {
                    const matchedMember = res.members.find(item => item.userName === currentUserName);

                    // 3. 如果找到了，就把该成员的 role 赋值给 userRole
                    if (matchedMember) {
                        this.userRole = matchedMember.role;
                    } else {
                        console.warn('在成员列表中未匹配到当前用户');
                        this.userRole = ''; // 或者给一个默认角色
                    }
                }
            } catch (e) {
                uni.showToast({ title: '加载失败', icon: 'none' });
            }
        },

        // 点击成员进入对应健康数据页面 [cite: 36]
        goToHealthData(member) {
            uni.navigateTo({
                url: `/pages/family/data?userId=${member.user_id}&userName=${member.nickname}&groupId=${this.groupInfo.groupId}`
            });
        },

        // 退出家庭组逻辑 [cite: 34]
        handleQuit() {
            uni.showModal({
                title: '提示',
                content: '确定要退出该家庭组吗？退出后将无法查看成员健康数据。',
                success: async (res) => {
                    if (res.confirm) {
                        const quitRes = await familyApi.quitGroup(this.groupInfo.groupId);
                        if (quitRes.code === 200) {
                            uni.reLaunch({ url: '/pages/index/index' });
                        }
                    }
                }
            });
        },

        navigateTo(url) {
            uni.navigateTo({ url });
        }
    }
};
</script>

<style scoped>
.members-container {
    padding: 30rpx;
    padding-top: 50rpx;
    background-color: #f7f8fa;
    min-height: 100vh;
}

/* 概况卡片 */
.group-card {
    background-color: #007AFF;
    border-radius: 20rpx;
    padding: 40rpx;
    color: white;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30rpx;
}

.group-name {
    font-size: 36rpx;
    font-weight: bold;
    display: block;
}

.group-id {
    font-size: 24rpx;
    opacity: 0.8;
}

.role-badge {
    font-size: 22rpx;
    padding: 4rpx 16rpx;
    border-radius: 10rpx;
    background: rgba(255, 255, 255, 0.2);
}

/* 管理入口 */
.admin-section {
    display: flex;
    background: white;
    padding: 30rpx;
    border-radius: 20rpx;
    margin-bottom: 30rpx;
}

.admin-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    font-size: 26rpx;
    position: relative;
}

.icon {
    font-size: 40rpx;
    margin-bottom: 10rpx;
}

.dot {
    width: 16rpx;
    height: 16rpx;
    background: #ff4d4f;
    border-radius: 50%;
    position: absolute;
    left: 50%;
    top: 0;
}

/* 列表样式 */
.list-title {
    margin: 20rpx 0;
}

.subtitle {
    font-size: 24rpx;
    color: #999;
    margin-left: 10rpx;
}

.member-list {
    background: white;
    border-radius: 20rpx;
    overflow: hidden;
}

.member-item {
    display: flex;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1rpx solid #f0f0f0;
}

.avatar {
    width: 80rpx;
    height: 80rpx;
    background: #e1f5fe;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #007AFF;
    font-weight: bold;
}

.member-detail {
    flex: 1;
    margin-left: 20rpx;
}

.name {
    font-size: 30rpx;
    color: #333;
    display: block;
}

.role-text {
    font-size: 22rpx;
    color: #999;
}

.arrow {
    color: #ccc;
    font-size: 24rpx;
}

/* 退出动作 */
.footer-action {
    margin-top: 60rpx;
    text-align: center;
}

.quit-btn {
    background: white;
    color: #ff4d4f;
    border: 1rpx solid #ff4d4f;
    border-radius: 50rpx;
    font-size: 28rpx;
    width: 60%;
}

.leader-tips {
    font-size: 24rpx;
    color: #999;
}

/* 弹窗遮罩 */
.mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.6);
    z-index: 999;
    display: flex;
    align-items: center;
    justify-content: center;
}

.modal-content {
    width: 85%;
    max-height: 70vh;
    background: white;
    border-radius: 24rpx;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.modal-header {
    padding: 30rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1rpx solid #eee;
}

.modal-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
}

.close-btn {
    padding: 10rpx;
    color: #999;
}

.modal-body {
    flex: 1;
    padding: 20rpx;
    background-color: #f9f9f9;
}

.apply-item {
    background: white;
    padding: 24rpx;
    border-radius: 16rpx;
    margin-bottom: 20rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.03);
}

.apply-user-box {
    display: flex;
    align-items: center;
    margin-bottom: 8rpx;
}

.apply-user {
    font-size: 30rpx;
    font-weight: bold;
    color: #007AFF;
}

.apply-type {
    font-size: 20rpx;
    background: #e1f5fe;
    color: #007AFF;
    padding: 2rpx 10rpx;
    border-radius: 6rpx;
    margin-left: 10rpx;
}

.apply-remark {
    font-size: 24rpx;
    color: #666;
    display: block;
}

.apply-time {
    font-size: 20rpx;
    color: #bbb;
    margin-top: 10rpx;
}

.apply-ops {
    display: flex;
    flex-direction: column;
    gap: 15rpx;
}

.op-btn {
    font-size: 24rpx;
    height: 54rpx;
    line-height: 54rpx;
    width: 120rpx;
    margin: 0;
    border-radius: 30rpx;
}

.approve {
    background: #007AFF;
    color: white;
}

.reject {
    background: #f5f5f5;
    color: #999;
}

.empty-apply {
    text-align: center;
    padding: 100rpx 0;
    color: #999;
    font-size: 26rpx;
}
</style>