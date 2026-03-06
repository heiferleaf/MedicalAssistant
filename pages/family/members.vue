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
                <image class="icon" src="../../static/Health/plus-circle.svg"/>
                <text>邀请成员</text>
            </view>
            <view class="admin-item" @click="navigateTo('/pages/family/list')">
                <image class="icon" src="../../static/Home/bell.svg"/>
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
            loading: false
        };
    },
    onShow() {
        this.fetchData();
    },
    methods: {
        async fetchData() {
            // 每次进入页面实时获取最新成员列表，不留缓存 
            try {
                let res = await familyApi.getMyGroup();
                this.groupInfo = res.group;
                this.memberList = res.members;
                // 1. 同步获取缓存中的用户名
                const currentUserName = uni.getStorageSync('userName');
                // console.log("当前用户:",currentUserName);

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
                url: `/pages/family/data?userId=${member.user_id}&userName=${member.nickname}`
            });
        },

        // 退出家庭组逻辑 [cite: 34]
        handleQuit() {
            uni.showModal({
                title: '提示',
                content: '确定要退出该家庭组吗？退出后将无法查看成员健康数据。',
                success: async (res) => {
                    if (res.confirm) {
                        const quitRes = await this.$api.quitGroup();
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
</style>