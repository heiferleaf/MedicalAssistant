<template>
    <view class="data-container">
        <view class="user-header">
            <view class="user-info">
                <text class="user-name">{{ userName }} 的健康看板</text>
                <text class="date-display">{{ selectedDate }}</text>
            </view>
            <picker mode="date" :value="selectedDate" @change="onDateChange">
                <button class="date-btn">切换日期</button>
            </picker>
        </view>

        <view class="section-title">药品服用情况</view>
        <view class="medicine-list">
            <view v-if="medicineList.length === 0" class="empty-tip">暂无服药记录</view>
            <view v-for="(med, index) in medicineList" :key="index" class="med-item" :class="statusClass(med.status)">
                <view class="med-main">
                    <text class="med-name">{{ med.name }}</text>
                    <text class="med-time">{{ med.timePoint }} · {{ med.dosage }}</text>
                </view>
                <view class="med-status">
                    <text class="status-tag">{{ statusText(med.status) }}</text>
                </view>
            </view>
        </view>

        <view class="section-title">生理指标</view>
        <view class="health-grid">
            <view class="health-card">
                <text class="label">血压 (mmHg)</text>
                <text class="value">{{ healthValue.bloodPressure || '--' }}</text>
            </view>
            <view class="health-card">
                <text class="label">血糖 (mmol/L)</text>
                <text class="value">{{ healthValue.bloodSugar || '--' }}</text>
            </view>
        </view>

        <view class="footer-link" @click="goToLogs">
            <text>查看该成员历史异常日志</text>
            <text class="arrow">></text>
        </view>
    </view>
</template>

<script>
import familyApi from '../../api/family';

export default {
    data() {
        return {
            userId: '',
            userName: '',
            groupId: -1,
            selectedDate: '', // 默认当天
            medicineList: [], // 对应 medicine_status 字段解析
            healthValue: {},  // 对应 health_value 字段解析
            loading: false
        };
    },
    onLoad(options) {
        // 从 members 页面跳转传参
        this.userId = options.userId;
        this.userName = options.userName || '成员';
        this.groupId = options.groupId;
        this.selectedDate = this.getTodayDate();

        // 监听 WebSocket 推送的正常服药消息，实时刷新 UI
        uni.$on('refreshHealthData', (data) => {
            if (data.memberName === this.userName) {
                this.fetchHealthData();
            }
        });
    },
    onShow() {
        this.fetchHealthData();
    },
    onUnload() {
        uni.$off('refreshHealthData');
    },
    methods: {
        getTodayDate() {
            return new Date().toISOString().split('T')[0];
        },

        // 切换日期重新拉取 API 数据 [cite: 75, 76]
        onDateChange(e) {
            this.selectedDate = e.detail.value;
            this.fetchHealthData();
        },

        async fetchHealthData() {
            this.loading = true;
            try {
                // 调用 API 获取 health_data 表中对应 user_id 和 record_date 的数据 [cite: 51, 52]
                const res = await familyApi.getGroupHealthData(this.groupId);
                if (res.data == null) {
                    res = {
                        code: 200,
                        message: "操作成功",
                        data: {
                            id: 3001,
                            user_id: 2,
                            group_id: 1001,
                            record_date: "2026-03-20",

                            // ⚠️ 注意是字符串格式（后端数据库就是这样）
                            medicine_status: JSON.stringify([
                                {
                                    name: "降压药A",
                                    timePoint: "08:00",
                                    dosage: "1片",
                                    status: 1   // 1=已服 2=漏服
                                },
                                {
                                    name: "降糖药B",
                                    timePoint: "12:00",
                                    dosage: "1片",
                                    status: 2
                                },
                                {
                                    name: "维生素C",
                                    timePoint: "20:00",
                                    dosage: "2片",
                                    status: 1
                                }
                            ]),

                            health_value: JSON.stringify({
                                bloodPressure: "120/80",
                                bloodSugar: "6.3"
                            })
                        }
                    };
                }

                if (res.data) {
                    // 解析存储在 JSON 字段中的服药和指标数据 [cite: 52]
                    this.medicineList = JSON.parse(res.data.medicine_status || '[]');
                    this.healthValue = JSON.parse(res.data.health_value || '{}');
                }
            } catch (e) {
                console.error(e);
                uni.showToast({ title: '加载失败', icon: 'none' });
            } finally {
                this.loading = false;
            }
        },

        statusText(status) {
            const map = { 0: '待服用', 1: '已服用', 2: '漏服' };
            return map[status] || '未知';
        },

        statusClass(status) {
            const map = { 0: 'status-wait', 1: 'status-done', 2: 'status-miss' };
            return map[status];
        },

        goToLogs() {
            uni.navigateTo({
                url: `/pages/health/abnormal?userId=${this.userId}`
            });
        }
    }
};
</script>

<style scoped>
.data-container {
    padding: 30rpx;
    background-color: #f8f9fa;
    min-height: 100vh;
}

.user-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: white;
    padding: 40rpx;
    border-radius: 20rpx;
    margin-bottom: 30rpx;
}

.user-name {
    font-size: 36rpx;
    font-weight: bold;
    display: block;
}

.date-display {
    font-size: 26rpx;
    color: #666;
    margin-top: 10rpx;
}

.date-btn {
    font-size: 24rpx;
    background: #e3f2fd;
    color: #007AFF;
    border: none;
}

.section-title {
    font-size: 30rpx;
    font-weight: bold;
    margin: 30rpx 10rpx 20rpx;
    color: #333;
}

/* 药品列表样式 */
.med-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: white;
    padding: 30rpx;
    border-radius: 16rpx;
    margin-bottom: 20rpx;
    border-left: 10rpx solid #ccc;
}

.med-name {
    font-size: 32rpx;
    font-weight: 500;
    display: block;
}

.med-time {
    font-size: 24rpx;
    color: #999;
    margin-top: 6rpx;
}

.status-tag {
    font-size: 24rpx;
    font-weight: bold;
}

/* 不同服药状态颜色 [cite: 65, 69] */
.status-wait {
    border-left-color: #ffa726;
}

.status-wait .status-tag {
    color: #ffa726;
}

.status-done {
    border-left-color: #66bb6a;
}

.status-done .status-tag {
    color: #66bb6a;
}

.status-miss {
    border-left-color: #ef5350;
}

.status-miss .status-tag {
    color: #ef5350;
}

/* 指标卡片 */
.health-grid {
    display: flex;
    gap: 20rpx;
}

.health-card {
    flex: 1;
    background: white;
    padding: 30rpx;
    border-radius: 16rpx;
    text-align: center;
}

.health-card .label {
    font-size: 24rpx;
    color: #999;
    display: block;
}

.health-card .value {
    font-size: 40rpx;
    font-weight: bold;
    color: #007AFF;
    margin-top: 10rpx;
}

.footer-link {
    margin-top: 50rpx;
    display: flex;
    justify-content: space-between;
    padding: 30rpx;
    background: #fff;
    border-radius: 16rpx;
    font-size: 28rpx;
    color: #666;
}
</style>