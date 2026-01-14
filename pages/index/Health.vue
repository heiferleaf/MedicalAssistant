<!-- pages/medication/medicine-box.vue -->
<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header">
      <view class="header-left">
        <text class="page-title">我的药箱</text>
      </view>
      <view class="header-right">
        <button class="calendar-btn" @click="switchView">
          <image
            class="calendar-icon"
            src="../../static/Health/calendar.svg"
            mode="aspectFit"
          />
          <text class="calendar-text">{{
            viewMode === "list" ? "日历视图" : "列表视图"
          }}</text>
        </button>
      </view>
    </view>

    <!-- 日历视图（折叠） -->
    <view class="calendar-view" v-if="viewMode === 'calendar' && showCalendar">
      <!-- 日历组件占位，可根据需求实现完整日历 -->
      <view class="calendar-placeholder">
        <text class="placeholder-text">日历视图</text>
        <text class="placeholder-desc">点击日期查看当日用药计划</text>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view
      class="main-content"
      scroll-y
      :show-scrollbar="false"
      :scroll-x="false"
    >
      <!-- 搜索栏 -->
      <view class="search-container">
        <view class="search-box">
          <image
            class="search-icon"
            src="../../static/Health/search.svg"
            mode="aspectFit"
          />
          <input
            class="search-input"
            type="text"
            placeholder="搜索药品"
            placeholder-class="placeholder"
            v-model="searchKeyword"
            @confirm="onSearch"
          />
          <button class="clear-btn" @click="clearSearch" v-if="searchKeyword">
            <image
              class="clear-icon"
              src="../../static/Health/close.svg"
              mode="aspectFit"
            />
          </button>
        </view>
      </view>

      <!-- 正在服用的药品 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">正在服用</text>
          <text class="section-count">({{ activeMedications.length }})</text>
        </view>

        <view class="medication-list">
          <view
            class="medication-card active"
            v-for="medication in activeMedications"
            :key="medication.id"
            @click="toMedicationDetail(medication.id)"
          >
            <view class="medication-header">
              <image
                class="pill-icon"
                src="../../static/Health/pill-active.svg"
                mode="aspectFit"
              />
              <view class="medication-name-wrapper">
                <text class="medication-name">{{ medication.name }}</text>
                <view class="medication-tags">
                  <view class="tag" v-for="tag in medication.tags" :key="tag">
                    <text class="tag-text">{{ tag }}</text>
                  </view>
                </view>
              </view>
              <view class="medication-status" :class="medication.statusClass">
                <text>{{ medication.statusText }}</text>
              </view>
            </view>

            <view class="medication-details">
              <view class="detail-item">
                <image
                  class="detail-icon"
                  src="../../static/Health/time.svg"
                  mode="aspectFit"
                />
                <text class="detail-text">{{ medication.schedule }}</text>
              </view>
              <view class="detail-item">
                <image
                  class="detail-icon"
                  src="../../static/Health/calendar-check.svg"
                  mode="aspectFit"
                />
                <text class="detail-text"
                  >已服用{{ medication.takenDays }}天 剩余{{
                    medication.remainingDays
                  }}天</text
                >
                <view class="missed-count" v-if="medication.missedCount > 0">
                  <text class="missed-text"
                    >⚠️漏服{{ medication.missedCount }}次</text
                  >
                </view>
              </view>
            </view>

            <view class="medication-actions">
              <button
                class="action-btn detail-btn"
                @click.stop="toMedicationDetail(medication.id)"
              >
                <text class="btn-text">详情</text>
              </button>
              <button
                class="action-btn reminder-btn"
                @click.stop="setReminder(medication.id)"
              >
                <text class="btn-text">提醒设置</text>
              </button>
              <button
                class="action-btn record-btn"
                @click.stop="recordDose(medication.id)"
                :class="{ recorded: medication.todayTaken }"
              >
                <text class="btn-text">{{
                  medication.todayTaken ? "今日已服" : "记录服用"
                }}</text>
              </button>
            </view>
          </view>
        </view>
      </view>

      <!-- 已停用的药品 -->
      <view class="section">
        <view class="section-header" @click="toggleInactive">
          <text class="section-title">已停用</text>
          <text class="section-count">({{ inactiveMedications.length }})</text>
          <view class="expand-btn">
            <text class="expand-text">{{
              showInactive ? "收起" : "展开"
            }}</text>
            <image
              class="expand-icon"
              :src="
                showInactive ? '../../static/Health/up.svg' : '../../static/Health/down.svg'
              "
              mode="aspectFit"
            />
          </view>
        </view>

        <view class="medication-list" v-if="showInactive">
          <view
            class="medication-card inactive"
            v-for="medication in inactiveMedications"
            :key="medication.id"
            @click="toMedicationDetail(medication.id)"
          >
            <view class="medication-header">
              <image
                class="pill-icon"
                src="../../static/Health/pill-inactive.svg"
                mode="aspectFit"
              />
              <view class="medication-name-wrapper">
                <text class="medication-name">{{ medication.name }}</text>
              </view>
              <view class="medication-status status-inactive">
                <text>已停用</text>
              </view>
            </view>

            <view class="medication-details">
              <view class="detail-item">
                <image
                  class="detail-icon"
                  src="../../static/Health/info.svg"
                  mode="aspectFit"
                />
                <text class="detail-text"
                  >停用时间: {{ medication.stopTime }}</text
                >
              </view>
              <view class="detail-item">
                <image
                  class="detail-icon"
                  src="../../static/Health/clock-history.svg"
                  mode="aspectFit"
                />
                <text class="detail-text"
                  >服用时长: {{ medication.duration }}</text
                >
              </view>
            </view>

            <view class="medication-actions">
              <button
                class="action-btn restart-btn"
                @click.stop="restartMedication(medication.id)"
              >
                <text class="btn-text">重新启用</text>
              </button>
              <button
                class="action-btn delete-btn"
                @click.stop="deleteMedication(medication.id)"
              >
                <text class="btn-text">删除</text>
              </button>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 添加药品按钮 -->
    <view class="add-button-container">
      <button class="add-button" @click="addMedication">
        <image
          class="add-icon"
          src="../../static/Health/plus-circle.svg"
          mode="aspectFit"
        />
        <text class="add-text">添加药品</text>
      </button>
    </view>
  </view>
</template>

<script>
export default {
  name: "HealthPage",
  data() {
    return {
      viewMode: "list", // 'list' 或 'calendar'
      showCalendar: false,
      searchKeyword: "",
      showInactive: false,
      activeMedications: [
        {
          id: 1,
          name: "阿司匹林肠溶片",
          tags: ["抗血小板", "处方药"],
          schedule: "每日1次 上午10:00",
          takenDays: 7,
          remainingDays: 23,
          missedCount: 0,
          todayTaken: true,
          statusClass: "status-normal",
          statusText: "按时服用",
        },
        {
          id: 2,
          name: "二甲双胍缓释片",
          tags: ["降糖药"],
          schedule: "每日2次 早晚饭后",
          takenDays: 3,
          remainingDays: 27,
          missedCount: 1,
          todayTaken: false,
          statusClass: "status-warning",
          statusText: "有漏服",
        },
        {
          id: 3,
          name: "阿托伐他汀钙片",
          tags: ["降脂药"],
          schedule: "每晚1次 睡前",
          takenDays: 15,
          remainingDays: 15,
          missedCount: 0,
          todayTaken: true,
          statusClass: "status-normal",
          statusText: "按时服用",
        },
      ],
      inactiveMedications: [
        {
          id: 4,
          name: "头孢克肟片",
          stopTime: "2025-12-15",
          duration: "7天",
          reason: "疗程结束",
        },
        {
          id: 5,
          name: "布洛芬缓释胶囊",
          stopTime: "2025-12-10",
          duration: "3天",
          reason: "症状缓解",
        },
      ],
    };
  },
  onLoad() {
    // 初始化数据
    this.loadMedications();
  },
  methods: {
    loadMedications() {
      // 这里可以调用API加载药品数据
      console.log("加载药品数据");
    },
    switchView() {
      this.viewMode = this.viewMode === "list" ? "calendar" : "list";
      this.showCalendar = this.viewMode === "calendar";
    },
    onSearch() {
      if (this.searchKeyword.trim()) {
        uni.showLoading({
          title: "搜索中...",
        });
        setTimeout(() => {
          uni.hideLoading();
          // 这里实现搜索逻辑
          console.log("搜索关键词:", this.searchKeyword);
        }, 500);
      }
    },
    clearSearch() {
      this.searchKeyword = "";
    },
    toggleInactive() {
      this.showInactive = !this.showInactive;
    },
    toMedicationDetail(id) {
      uni.navigateTo({
        url: `/pages/medication/detail?id=${id}`,
      });
    },
    setReminder(id) {
      uni.navigateTo({
        url: `/pages/medication/reminder?id=${id}`,
      });
    },
    recordDose(id) {
      const medication = this.activeMedications.find((m) => m.id === id);
      if (medication) {
        medication.todayTaken = !medication.todayTaken;
        uni.showToast({
          title: medication.todayTaken ? "已记录服用" : "已取消服用记录",
          icon: "success",
        });
      }
    },
    restartMedication(id) {
      uni.showModal({
        title: "重新启用药品",
        content: "确定要重新启用这个药品吗？",
        success: (res) => {
          if (res.confirm) {
            const index = this.inactiveMedications.findIndex(
              (m) => m.id === id
            );
            if (index !== -1) {
              const medication = this.inactiveMedications.splice(index, 1)[0];
              // 这里可以调用API更新药品状态
              uni.showToast({
                title: "药品已重新启用",
                icon: "success",
              });
            }
          }
        },
      });
    },
    deleteMedication(id) {
      uni.showModal({
        title: "删除药品",
        content: "确定要删除这个药品记录吗？",
        confirmColor: "#FF6B6B",
        success: (res) => {
          if (res.confirm) {
            const index = this.inactiveMedications.findIndex(
              (m) => m.id === id
            );
            if (index !== -1) {
              this.inactiveMedications.splice(index, 1);
              uni.showToast({
                title: "删除成功",
                icon: "success",
              });
            }
          }
        },
      });
    },
    addMedication() {
      uni.navigateTo({
        url: "/pages/medication/add",
      });
    },
  },
};
</script>

<style scoped lang="scss">
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8faff 0%, #ffffff 100%);
  padding-bottom: 160rpx;
}

/* 头部样式 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 32rpx 24rpx;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 0 0 32rpx 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.15);
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1rpx;
}

.header-right {
  display: flex;
  align-items: center;
}

.calendar-btn {
  display: flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 24rpx;
  border: none;
  backdrop-filter: blur(10rpx);
  transition: all 0.3s ease;
}

.calendar-btn:active {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(0.95);
}

.calendar-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 8rpx;
}

.calendar-text {
  font-size: 26rpx;
  color: #ffffff;
  font-weight: 500;
}

/* 日历视图 */
.calendar-view {
  padding: 32rpx;
}

.calendar-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 24rpx;
  border: 2rpx dashed rgba(77, 142, 255, 0.3);
}

.placeholder-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #4d8eff;
  margin-bottom: 12rpx;
}

.placeholder-desc {
  font-size: 26rpx;
  color: #87909c;
}

/* 主要内容区域 */
.main-content {
  height: calc(100vh - 240rpx);
  padding: 32rpx;
  box-sizing: border-box;
  overflow-x: hidden !important;
}

.main-content::-webkit-scrollbar {
  display: none;
}

/* 搜索栏 */
.search-container {
  margin-bottom: 40rpx;
}

.search-box {
  display: flex;
  align-items: center;
  padding: 0 28rpx;
  background: #ffffff;
  border-radius: 24rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.2);
  box-shadow: 0 4rpx 16rpx rgba(45, 107, 255, 0.08);
}

.search-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 16rpx;
}

.search-input {
  flex: 1;
  height: 88rpx;
  font-size: 30rpx;
  color: #2d3b4e;
}

.placeholder {
  color: #b4bfd3;
  font-size: 30rpx;
}

.clear-btn {
  width: 40rpx;
  height: 40rpx;
  padding: 0;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-icon {
  width: 24rpx;
  height: 24rpx;
}

/* 分区样式 */
.section {
  margin-bottom: 40rpx;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  padding: 0 8rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2d3b4e;
}

.section-count {
  font-size: 28rpx;
  font-weight: 600;
  color: #4d8eff;
  margin-left: 12rpx;
}

.expand-btn {
  display: flex;
  align-items: center;
  margin-left: auto;
  padding: 8rpx 16rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 20rpx;
  transition: all 0.3s ease;
}

.expand-btn:active {
  background: rgba(77, 142, 255, 0.2);
}

.expand-text {
  font-size: 26rpx;
  color: #4d8eff;
  margin-right: 8rpx;
}

.expand-icon {
  width: 24rpx;
  height: 24rpx;
}

/* 药品卡片 */
.medication-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.medication-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
  transition: all 0.3s ease;
}

.medication-card.active {
  border-left: 8rpx solid #4d8eff;
}

.medication-card.inactive {
  border-left: 8rpx solid #b4bfd3;
  opacity: 0.8;
}

.medication-card:active {
  transform: translateY(-2rpx);
  box-shadow: 0 8rpx 32rpx rgba(45, 107, 255, 0.12);
}

/* 药品头部 */
.medication-header {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24rpx;
}

.pill-icon {
  width: 48rpx;
  height: 48rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.medication-name-wrapper {
  flex: 1;
}

.medication-name {
  font-size: 34rpx;
  font-weight: 700;
  color: #2d3b4e;
  margin-bottom: 12rpx;
  display: block;
}

.medication-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag {
  padding: 6rpx 16rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 16rpx;
}

.tag-text {
  font-size: 22rpx;
  color: #4d8eff;
  font-weight: 500;
}

.medication-status {
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;

  &.status-normal {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
  }

  &.status-warning {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }

  &.status-inactive {
    background: rgba(180, 191, 211, 0.1);
    color: #87909c;
  }
}

/* 药品详情 */
.medication-details {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 32rpx;
  padding-left: 68rpx; /* 对齐图标 */
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-icon {
  width: 28rpx;
  height: 28rpx;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.detail-text {
  font-size: 28rpx;
  color: #555e6d;
  flex: 1;
}

.missed-count {
  margin-left: 16rpx;
}

.missed-text {
  font-size: 24rpx;
  color: #ff6b6b;
  font-weight: 500;
}

/* 药品操作按钮 */
.medication-actions {
  display: flex;
  gap: 20rpx;
  padding-left: 68rpx; /* 对齐图标 */
}

.action-btn {
  flex: 1;
  padding: 20rpx 0;
  border-radius: 20rpx;
  border: none;
  font-size: 28rpx;
  font-weight: 600;
  transition: all 0.3s ease;

  &:active {
    transform: scale(0.95);
  }
}

.detail-btn {
  background: linear-gradient(
    135deg,
    rgba(77, 142, 255, 0.1) 0%,
    rgba(45, 107, 255, 0.05) 100%
  );
  color: #4d8eff;
}

.reminder-btn {
  background: linear-gradient(
    135deg,
    rgba(245, 158, 11, 0.1) 0%,
    rgba(245, 158, 11, 0.05) 100%
  );
  color: #f59e0b;
}

.record-btn {
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  color: #ffffff;

  &.recorded {
    background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
  }
}

.restart-btn {
  background: linear-gradient(
    135deg,
    rgba(77, 142, 255, 0.1) 0%,
    rgba(45, 107, 255, 0.05) 100%
  );
  color: #4d8eff;
}

.delete-btn {
  background: linear-gradient(
    135deg,
    rgba(255, 107, 107, 0.1) 0%,
    rgba(255, 107, 107, 0.05) 100%
  );
  color: #ff6b6b;
}

.btn-text {
  font-size: 26rpx;
  font-weight: 600;
}

/* 添加药品按钮 */
.add-button-container {
  position: fixed;
  bottom: 120rpx;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  padding: 0 32rpx;
  z-index: 1000;
}

.add-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 400rpx;
  padding: 28rpx 0;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 28rpx;
  border: none;
  box-shadow: 0 8rpx 32rpx rgba(45, 107, 255, 0.3);
  transition: all 0.3s ease;
}

.add-button:active {
  transform: scale(0.95);
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.4);
}

.add-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 12rpx;
}

.add-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #ffffff;
}
</style>
