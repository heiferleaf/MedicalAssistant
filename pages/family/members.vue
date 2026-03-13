<template>
    <view class="page-container">
        <view class="header">
            <view class="header-left">
                <view class="icon-btn" @click="uni.navigateBack()">
                    <image class="icon-md" src="../../static/Register/back.png" mode="aspectFit"></image>
                </view>
                <text class="header-title">家庭管理</text>
            </view>
            <view class="icon-btn" @click="goToSettings">
                <image class="icon-md" src="../../static/Mine/settings.svg" mode="aspectFit"></image>
            </view>
        </view>

        <scroll-view scroll-y class="main-content">

            <view class="section-padding mt-4">
                <view class="family-card">
                    <view class="card-info">
                        <view class="cover-image-wrapper">
                            <image class="cover-image" src="../../static/family/family_cover.png" mode="aspectFill">
                            </image>
                        </view>
                        <view class="text-info">
                            <text class="tag">主家庭组</text>
                            <text class="title">幸福一家人</text>
                            <text class="desc">家庭 ID: 4</text>
                        </view>
                    </view>
                    <button class="btn-manage" hover-class="btn-manage-hover" @click="manageDetails">
                        管理小组详情
                    </button>
                </view>
            </view>

            <view class="section-padding action-grid">
                <view class="action-btn" hover-class="action-btn-hover"
                    @click="navigateTo(`/pages/family/invite?groupId=${groupInfo.groupId}`)">
                    <image class="icon-sm mr-2" src="../../static/family/person_add.svg" mode="aspectFit"></image>
                    <text class="action-text">邀请成员</text>
                </view>
                <view class="action-btn" hover-class="action-btn-hover" @click="openApplyCenter">
                    <image class="icon-sm mr-2" src="../../static/family/verified_user.svg" mode="aspectFit"></image>
                    <text class="action-text">审批中心</text>
                </view>
            </view>

            <view class="section-padding members-section">
                <view class="section-header">
                    <text class="section-title">家庭成员</text>
                    <view class="badge"><text class="badge-text">共 {{ memberList.length }} 人</text></view>
                </view>

                <view class="members-list">
                    <view class="member-item" v-for="member in memberList" :key="member.id"
                        hover-class="member-item-hover" @click="goToHealthData(member)">
                        <view class="member-info">
                            <view class="avatar-wrapper">
                                <image class="avatar" :class="{ 'avatar-leader': member.role == 'leader' }"
                                    :src="this.getAvatar(member.userId)" mode="aspectFill"></image>
                                <view v-if="member.role == 'leader'" class="leader-badge">★</view>
                            </view>
                            <view class="name-role">
                                <text class="name">{{ member.userName }}</text>
                                <text class="role" :class="{ 'role-leader': member.role == 'leader' }">{{ member.role
                                    }}</text>
                            </view>
                        </view>
                        <image class="icon-sm icon-chevron" src="../../static/Home/right-arrow.svg" mode="aspectFit">
                        </image>
                    </view>
                </view>
            </view>

            <button v-if="userRole == 'normal'" class="btn-manage" hover-class="btn-manage-hover" @click="handleQuit">
                退出家庭组
            </button>
            <view class="section-padding footer-note">
                <view class="note-box">
                    <text class="note-text">如需解散家庭组或转让组长角色，请访问高级设置。成员可以随时退出小组，但将失去对共享主资源的访问权限。</text>
                </view>
            </view>

            <view class="bottom-spacer"></view>
        </scroll-view>
        <view class="modal-overlay" v-if="showApplyModal" @click="showApplyModal = false">
            <view class="modal-content" @click.stop>
                <view class="modal-header">
                    <text class="modal-title">审批中心</text>
                    <view class="close-btn" @click="showApplyModal = false">
                        <text class="close-icon">✕</text>
                    </view>
                </view>

                <scroll-view scroll-y class="apply-list-scroll">
                    <view v-if="applyList.length === 0" class="empty-state">
                        <text class="empty-text">暂无新的申请记录</text>
                    </view>

                    <view class="apply-card" v-for="apply in applyList" :key="apply.applyId">
                        <view class="apply-top">
                            <image class="apply-avatar" :src="getAvatar(apply.userName || apply.userNickname)"
                                mode="aspectFill"></image>

                            <view class="apply-info">
                                <view class="name-time">
                                    <text class="name">{{ apply.userName || apply.userNickname }}</text>
                                    <text class="time">{{ apply.createTime.substring(0, 10) }}</text>
                                </view>
                                <text class="remark">“{{ apply.remark || '申请加入您的家庭组' }}”</text>
                            </view>
                        </view>

                        <view class="apply-actions" v-if="apply.status === 'pending'">
                            <button class="btn-reject" hover-class="btn-reject-hover"
                                @click="handleApply(apply, 'reject')">拒绝</button>
                            <button class="btn-agree" hover-class="btn-agree-hover"
                                @click="handleApply(apply, 'accept')">同意</button>
                        </view>

                        <view class="apply-status" v-else>
                            <text class="status-text">{{ apply.status === 'agreed' ? '已同意' : '已拒绝' }}</text>
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
        getAvatar(userId) {
            return `https://api.dicebear.com/7.x/adventurer/svg?seed=${userId}`;
        },
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
            console.log(`处理申请 ${apply.applyId}，操作: ${action}`);
            uni.showLoading({ title: '处理中...' });
            try {
                // 假设你的 api 有这个方法，参数为 applyId 和 动作
                let params = {
                    opType: action,
                    remark: "OK",
                    applyId: apply.applyId
                }
                const res = await familyApi.approveApply(this.groupInfo.groupId, params);

                if (res.code === 200) {
                    uni.showToast({ title: '处理成功' });
                    console.log("申请处理结果: ", res);
                    // this.getMessage(); // 刷新列表
                    // this.fetchData(); // 刷新成员列表（如果同意了人的话）
                    if (this.applyList.length <= 1) this.showApplyModal = false;
                }
            } catch (e) {
                console.error("报错显示:", e);
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
<style lang="scss" scoped>
/* 全局变量 */
$primary: #4d88ff;
$bg-color: #f5f6f8;
$card-bg: #ffffff;
$text-main: #0f172a;
$text-sub: #64748b;
$border-color: #e2e8f0;

/* 通用图标尺寸 */
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

.icon-tab {
    width: 48rpx;
    height: 48rpx;
    flex-shrink: 0;
    margin-bottom: 8rpx;
}

.mr-2 {
    margin-right: 16rpx;
}

.mt-4 {
    margin-top: 32rpx;
}

.section-padding {
    padding: 0 48rpx;
    margin-bottom: 48rpx;
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

    .header-left {
        display: flex;
        align-items: center;
        gap: 24rpx;
    }

    .icon-btn {
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
    }
}

/* 主内容区 */
.main-content {
    flex: 1;
    height: 0;
}

/* 家庭组卡片 */
.family-card {
    background-color: rgba($primary, 0.1);
    border: 2rpx solid rgba($primary, 0.2);
    border-radius: 32rpx;
    padding: 40rpx;

    .card-info {
        display: flex;
        align-items: center;
        gap: 32rpx;
        margin-bottom: 40rpx;

        .cover-image-wrapper {
            width: 128rpx;
            height: 128rpx;
            border-radius: 24rpx;
            background-color: $primary;
            box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
            overflow: hidden;
            flex-shrink: 0;

            .cover-image {
                width: 100%;
                height: 100%;
            }
        }

        .text-info {
            display: flex;
            flex-direction: column;

            .tag {
                font-size: 20rpx;
                font-weight: 600;
                text-transform: uppercase;
                color: $primary;
                letter-spacing: 2rpx;
            }

            .title {
                font-size: 40rpx;
                font-weight: bold;
                color: $text-main;
                margin: 8rpx 0;
            }

            .desc {
                font-size: 28rpx;
                color: $text-sub;
            }
        }
    }

    .btn-manage {
        background-color: $primary;
        color: #ffffff;
        font-size: 30rpx;
        font-weight: 600;
        height: 88rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 20rpx;
        box-shadow: 0 8rpx 16rpx rgba($primary, 0.2);

        &::after {
            border: none;
        }
    }

    .btn-manage-hover {
        background-color: #3b76eb;
    }
}

/* 操作网格 */
.action-grid {
    display: flex;
    gap: 24rpx;

    .action-btn {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: #f8fafc;
        border: 2rpx solid $border-color;
        height: 100rpx;
        border-radius: 24rpx;
        transition: all 0.2s;

        .action-text {
            font-size: 28rpx;
            font-weight: 600;
            color: $text-main;
        }
    }

    .action-btn-hover {
        background-color: #e2e8f0;
    }
}

/* 成员列表 */
.members-section {
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
            background-color: #f1f5f9;
            padding: 6rpx 16rpx;
            border-radius: 8rpx;

            .badge-text {
                color: $text-main;
                font-size: 24rpx;
                font-weight: bold;
            }
        }
    }

    .members-list {
        display: flex;
        flex-direction: column;
        gap: 24rpx;

        .member-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 32rpx;
            background-color: $card-bg;
            border: 2rpx solid #f1f5f9;
            border-radius: 24rpx;
            box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.02);

            .member-info {
                display: flex;
                align-items: center;
                gap: 24rpx;

                .avatar-wrapper {
                    position: relative;

                    .avatar {
                        width: 80rpx;
                        height: 80rpx;
                        border-radius: 50%;
                        background-color: #f1f5f9;
                    }

                    .avatar-leader {
                        border: 4rpx solid $primary;
                        box-sizing: border-box;
                    }

                    .leader-badge {
                        position: absolute;
                        bottom: -4rpx;
                        right: -4rpx;
                        background-color: $primary;
                        color: white;
                        font-size: 16rpx;
                        width: 28rpx;
                        height: 28rpx;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        border-radius: 50%;
                        border: 4rpx solid $card-bg;
                    }
                }

                .name-role {
                    display: flex;
                    flex-direction: column;

                    .name {
                        font-size: 28rpx;
                        font-weight: bold;
                        color: $text-main;
                    }

                    .role {
                        font-size: 20rpx;
                        font-weight: bold;
                        text-transform: uppercase;
                        letter-spacing: 2rpx;
                        color: $text-sub;
                        margin-top: 4rpx;
                    }

                    .role-leader {
                        color: $primary;
                    }
                }
            }

            .icon-chevron {
                opacity: 0.3;
            }
        }

        .member-item-hover {
            background-color: #f8fafc;
        }
    }
}

/* 底部提示 */
.footer-note {
    .note-box {
        background-color: rgba(#94a3b8, 0.1);
        border-radius: 16rpx;
        padding: 32rpx;
        margin: 32rpx;
        text-align: center;

        .note-text {
            font-size: 24rpx;
            color: $text-sub;
            line-height: 1.6;
        }
    }
}

.bottom-spacer {
    height: 160rpx;
}

.btn-manage {
    background-color: $primary;
    color: #ffffff;
    font-size: 30rpx;
    font-weight: 600;
    width: 80%;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 20rpx;
    box-shadow: 0 8rpx 16rpx rgba($primary, 0.2);

    &::after {
        border: none;
    }
}

/* 弹窗蒙层 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 999;
  display: flex;
  flex-direction: column;
  justify-content: flex-end; /* 默认让弹窗在底部，也可改为 center 让它居中 */
}

/* 弹窗内容容器 */
.modal-content {
  background-color: #ffffff;
  border-radius: 40rpx 40rpx 0 0;
  width: 100%;
  max-height: 80vh; /* 最高不超过屏幕 80% */
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

/* 弹窗头部 */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 40rpx;
  border-bottom: 2rpx solid #f1f5f9;
  
  .modal-title {
    font-size: 36rpx;
    font-weight: bold;
    color: #0f172a;
  }
  
  .close-btn {
    width: 64rpx;
    height: 64rpx;
    background-color: #f8fafc;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .close-icon {
      font-size: 32rpx;
      color: #64748b;
    }
  }
}

/* 滚动列表区 */
.apply-list-scroll {
  flex: 1;
  padding: 32rpx 40rpx;
  min-height: 400rpx;
}

.empty-state {
  display: flex;
  justify-content: center;
  padding-top: 100rpx;
  .empty-text { color: #94a3b8; font-size: 28rpx; }
}

/* 单个申请卡片 */
.apply-card {
  width: 100%; /* 确保它占满父容器可用宽度 */
  box-sizing: border-box; /* 核心修复：让内边距和边框向内收缩，不往外撑 */
  
  width: 90%;
  background-color: #f8fafc;
  border: 2rpx solid #e2e8f0;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  
  .apply-top {
    display: flex;
    gap: 24rpx;
    margin-bottom: 32rpx;
    
    .apply-avatar {
      width: 96rpx;
      height: 96rpx;
      border-radius: 50%;
      background-color: #e2e8f0;
      flex-shrink: 0;
    }
    
    .apply-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;
      
      .name-time {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8rpx;
        
        .name { font-size: 32rpx; font-weight: bold; color: #0f172a; }
        .time { font-size: 24rpx; color: #94a3b8; }
      }
      
      .remark {
        font-size: 26rpx;
        color: #64748b;
        font-style: italic;
        line-height: 1.4;
      }
    }
  }
  
  /* 操作按钮 */
  .apply-actions {
    display: flex;
    gap: 24rpx;
    
    .btn-reject, .btn-agree {
      flex: 1;
      height: 80rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 16rpx;
      font-size: 28rpx;
      font-weight: 600;
      margin: 0; /* 抹平原生 button 默认 margin */
      &::after { border: none; }
    }
    
    .btn-reject {
      background-color: #ffffff;
      color: #475569;
      border: 2rpx solid #cbd5e1;
    }
    .btn-reject-hover { background-color: #fee2e2; color: #dc2626; border-color: #fca5a5; }
    
    .btn-agree {
      background-color: #4d88ff;
      color: #ffffff;
    }
    .btn-agree-hover { opacity: 0.9; transform: scale(0.98); }
  }
  
  /* 已处理状态文本 */
  .apply-status {
    text-align: right;
    padding-top: 16rpx;
    border-top: 2rpx dashed #cbd5e1;
    .status-text { font-size: 26rpx; color: #94a3b8; }
  }
}
</style>