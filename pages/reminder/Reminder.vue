<template>
  <view class="container">
    <view class="custom-navbar">
      <view class="nav-header">
        <view class="nav-back" @click="uni.navigateBack()">
          <view class="back-icon"></view>
        </view>
        <view class="date-badge">
          <text>{{ currentMonth }}</text>
        </view>
      </view>
      <view class="nav-search">
        <view class="search-inner">
          <icon type="search" size="18" color="#9FA0AB" class="search-icon" />
          <input type="text" v-model="historyParams.medicineName" placeholder="搜索药品名称..."
            placeholder-class="search-placeholder" @confirm="fetchHistory(true)" confirm-type="search" />
          <text v-if="historyParams.medicineName" class="clear-btn" @click="
            historyParams.medicineName = '';
          fetchHistory(true);
          ">×</text>
        </view>
      </view>

      <view class="tab-pill-container">
        <view class="tab-pill" :class="{ active: currentTab === 0 }" @click="currentTab = 0">今日任务</view>
        <view class="tab-pill" :class="{ active: currentTab === 1 }" @click="currentTab = 1">用药计划</view>
        <view class="tab-pill" :class="{ active: currentTab === 2 }" @click="handleSwitchHistory">历史任务</view>
      </view>
    </view>

    <scroll-view scroll-y class="content-area" v-if="currentTab === 0">
      <view class="calendar-strip">
        <scroll-view scroll-x class="calendar-scroll" show-scrollbar="false">
          <view class="calendar-row">
            <view class="date-item" v-for="(day, index) in weekDays" :key="index">
              <text class="week-text">{{ day.week }}</text>
              <view class="day-circle" :class="{ 'date-active': day.fullDate === todayDate }">
                <text class="day-text">{{ day.day }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="section-header">
        <text class="section-title">今日待服</text>
        <view class="badge-count" v-if="pendingCount > 0">{{
          pendingCount
        }}</view>
      </view>

      <view class="timeline-container">
        <view v-if="taskList.length === 0" class="empty-tip">
          今日暂无任务，好好休息
        </view>

        <view class="timeline-row" v-for="(task, index) in taskList" :key="task.id"
          @click="handleTaskClick(task, index)">
          <view class="time-column">
            <text class="time-text">{{ task.timePoint }}</text>
          </view>

          <view class="card-column" :class="{
            'card-active': task.status !== 1,
            'card-done': task.status === 1,
          }">
            <view class="card-content">
              <text class="card-title">{{ task.medicineName }}</text>
              <text class="card-desc">剂量: {{ task.dosage }}</text>

              <view class="card-footer">
                <view class="status-pill" v-if="task.status === 1">
                  <text class="icon-check">✓</text> 已服用
                </view>
                <view class="status-pill pending" v-else-if="task.status === 2">
                  漏服
                </view>
                <view class="status-pill pending" v-else> 待服用 </view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view style="height: 40rpx"></view>
    </scroll-view>

    <scroll-view scroll-y class="content-area" v-if="currentTab === 1">
      <view class="plan-list-container">
        <view class="plan-card-modern" v-for="(plan, index) in planList" :key="plan.id">
          <view class="plan-top">
            <image class="icon" src="/static/Health/pill-active.svg" />
            <view class="plan-info">
              <text class="p-name">{{ plan.medicineName }}</text>
              <text class="p-dosage">每次 {{ plan.dosage }}</text>
            </view>
            <view class="plan-opt">
              <image class="opt-btn" @click.stop="openPlanModal('edit', plan)" style="width: 32rpx;height: 32rpx;" src="/static/Prepare/edit.svg" />
              <image class="opt-btn delete" @click.stop="handleDeletePlan(plan.id, index)" style="width: 32rpx;height: 32rpx;" src="/static/Reminder/delete.svg" />
            </view>
          </view>

          <view class="plan-times">
            <text class="time-chip" v-for="(t, ti) in plan.timePoints" :key="ti">{{ t }}</text>
          </view>

          <view class="plan-dates">
            📅 {{ plan.startDate }} 至 {{ plan.endDate || "长期" }}
          </view>
        </view>
      </view>

      <view class="fab-add-btn" @click="openPlanModal('add')">
        <text class="plus-icon">+</text>
      </view>
    </scroll-view>

    <scroll-view scroll-y class="content-area" v-if="currentTab === 2">
      <view class="history-filter">
        <picker mode="date" :value="historyParams.start" @change="
          (e) => {
            historyParams.start = e.detail.value;
            fetchHistory(true);
          }
        ">
          <view class="filter-item">{{
            historyParams.start || "起始日期"
          }}</view>
        </picker>
        <text class="filter-sep">-</text>
        <picker mode="date" :value="historyParams.end" @change="
          (e) => {
            historyParams.end = e.detail.value;
            fetchHistory(true);
          }
        ">
          <view class="filter-item">{{ historyParams.end || "截止日期" }}</view>
        </picker>
      </view>

      <view class="history-list">
        <view class="history-card" v-for="item in historyList" :key="item.id">
          <view class="h-main">
            <view class="h-info">
              <text class="h-name">{{ item.medicineName }}</text>
              <text class="h-time">{{ item.taskDate }} {{ item.timePoint }}</text>
            </view>
            <view class="h-status" :class="item.status === 1 ? 'st-done' : 'st-miss'">
              {{ item.status === 1 ? "已服" : "未服" }}
            </view>
          </view>
          <view class="h-footer" v-if="item.operateTime">
            记录时间: {{ formatTime(item.operateTime) }}
          </view>
        </view>
        <view v-if="historyList.length === 0" class="empty-tip">暂无相关历史记录</view>
      </view>
    </scroll-view>

    <view class="modal-mask" v-if="showModal">
      <view class="modal-content-modern">
        <view class="modal-header">
          <text class="m-title">{{
            modalType === "add" ? "新增计划" : "编辑计划"
          }}</text>
        </view>

        <input class="input-modern" v-model="formData.medicineName" placeholder="药品名称" />
        <input class="input-modern" v-model="formData.dosage" placeholder="剂量 (如: 2粒)" />

        <view class="picker-row">
          <picker mode="date" :value="formData.startDate" @change="(e) => (formData.startDate = e.detail.value)">
            <view class="picker-btn">开始: {{ formData.startDate || "选择" }}</view>
          </picker>
          <picker mode="date" :value="formData.endDate" @change="(e) => (formData.endDate = e.detail.value)">
            <view class="picker-btn">结束: {{ formData.endDate || "可选" }}</view>
          </picker>
        </view>

        <view class="time-config">
          <text class="label">时间点:</text>
          <view class="tags-wrapper">
            <view class="t-tag" v-for="(t, i) in formData.timePoints" :key="i" @click="removeTimePoint(i)">{{ t }} ×
            </view>
            <picker mode="time" @change="addTimePoint">
              <view class="add-tag-btn">+ 添加</view>
            </picker>
          </view>
        </view>

        <input class="input-modern" v-model="formData.remark" placeholder="备注" />

        <view class="modal-actions">
          <button class="btn-cancel" @click="showModal = false">取消</button>
          <button class="btn-save" @click="submitPlan">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import reminderApi from "../../api/reminder.js";

export default {
  data() {
    return {
      currentTab: 0,
      todayDate: new Date().toISOString().split("T")[0],
      currentMonth: "",
      weekDays: [], // 存放日历数据
      taskList: [],
      planList: [],
      showModal: false,
      modalType: "add",
      formData: {
        id: null,
        medicineName: "",
        dosage: "",
        startDate: "",
        endDate: "",
        timePoints: [],
        remark: "",
      },
      userId: null,
      // 历史记录相关数据
      historyList: [],
      historyParams: {
        userId: null,
        start: "",
        end: "",
        medicineName: "",
        status: null,
      },
      searchQuery: "",
    };
  },
  computed: {
    // 计算待办数量
    pendingCount() {
      return this.taskList.filter((t) => t.status !== 1).length;
    },
  },
  onLoad(options) {
    this.initCalendar(); // 初始化日历条
    this.initData();

    if(options.tab === "history") {
      this.currentTab = 2;
      this.handleSwitchHistory();
    }
  },
  methods: {
    // --- 新增：生成日历条数据 ---
    initCalendar() {
      const date = new Date();
      const options = { month: "short", year: "numeric" };
      this.currentMonth = date.toLocaleDateString("en-US", options); // 如 "Dec 2025"

      // 生成包含今天的一周数据（前后各几天）
      const days = [];
      const weekMap = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

      // 简单生成从今天开始往后7天，或者你可以做成当前周
      for (let i = -2; i < 5; i++) {
        const d = new Date();
        d.setDate(date.getDate() + i);
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, "0");
        const dd = String(d.getDate()).padStart(2, "0");

        days.push({
          week: weekMap[d.getDay()],
          day: d.getDate(),
          fullDate: `${yyyy}-${mm}-${dd}`,
        });
      }
      this.weekDays = days;
    },

    getUserId() {
      const userId = uni.getStorageSync("userId") || 1; // 默认值方便调试
      this.userId = userId;
      this.historyParams.userId = this.userId;
    },

    async initData() {
      this.getUserId();
      await this.fetchTasks();
      await this.fetchPlans();
    },

    // --- 任务逻辑 (接口调用保持不变) ---
    async fetchTasks() {
      try {
        const res = await reminderApi.getTodayTasks(this.userId);
        if (res && res.code === 200) {
          // 假设 api 返回结构里 data 才是数组，或者根据你的封装 res 直接是数组
          const list = res.data || [];
          this.taskList = list.sort((a, b) =>
            a.timePoint.localeCompare(b.timePoint),
          );
        } else if (Array.isArray(res)) {
          // 兼容你的封装直接返回 data 的情况
          this.taskList = res.sort((a, b) =>
            a.timePoint.localeCompare(b.timePoint),
          );
        }
      } catch (e) {
        console.error("获取任务失败", e);
      }
    },

    async handleTaskClick(task, index) {
      if (task.status === 1 || task.status === 2) return;

      try {
        const res = await reminderApi.updateTaskStatus(task.id, this.userId, 1);
        this.taskList[index].status = 1;
        this.taskList[index].operateTime = new Date().toISOString();
        uni.showToast({ title: "已服药", icon: "success" });
      } catch (e) {
        uni.showToast({ title: "操作失败", icon: "none" });
      }
    },

    // --- 计划逻辑 ---
    async fetchPlans() {
      try {
        const res = await reminderApi.getPlanList(this.userId);
        let list = [];
        if (res && res.code === 200) list = res.data;
        else if (Array.isArray(res)) list = res;

        this.planList = list.map((item) => {
          if (typeof item.timePoints === "string") {
            try {
              item.timePoints = JSON.parse(item.timePoints);
            } catch (e) { }
          }
          return item;
        });
      } catch (e) {
        console.error(e);
      }
    },

    openPlanModal(type, plan = null) {
      this.modalType = type;
      if (type === "edit" && plan) {
        this.formData = JSON.parse(JSON.stringify(plan));
      } else {
        this.formData = {
          medicineName: "",
          dosage: "",
          startDate: this.todayDate,
          endDate: "",
          timePoints: [],
          remark: "",
        };
      }
      this.showModal = true;
    },

    addTimePoint(e) {
      const time = e.detail.value;
      if (!this.formData.timePoints.includes(time)) {
        this.formData.timePoints.push(time);
        this.formData.timePoints.sort();
      }
    },

    removeTimePoint(index) {
      this.formData.timePoints.splice(index, 1);
    },

    async submitPlan() {
      if (
        !this.formData.medicineName ||
        this.formData.timePoints.length === 0
      ) {
        return uni.showToast({ title: "请填写完整信息", icon: "none" });
      }
      try {
        if (this.modalType === "add") {
          const res = await reminderApi.addPlan(this.userId, this.formData);
          // 兼容返回格式
          const newPlan = res.data ? res.data : res;
          this.planList.push(newPlan);
          if (this.formData.startDate === this.todayDate) {
            this.fetchTasks();
          }
          uni.showToast({ title: "添加成功" });
        } else {
          await reminderApi.editPlan(
            this.formData.id,
            this.userId,
            this.formData,
          );
          const idx = this.planList.findIndex((p) => p.id === this.formData.id);
          if (idx !== -1) {
            this.planList.splice(idx, 1, this.formData);
          }
          uni.showToast({ title: "修改成功" });
        }
        this.showModal = false;
      } catch (e) {
        uni.showToast({ title: "提交失败", icon: "none" });
      }
    },

    async handleDeletePlan(planId, index) {
      uni.showModal({
        title: "确认删除",
        content: "删除计划将取消后续所有提醒",
        success: async (res) => {
          if (res.confirm) {
            try {
              await reminderApi.deletePlan(planId, this.userId);
              this.planList.splice(index, 1);
              this.fetchTasks();
              uni.showToast({ title: "已删除" });
            } catch (e) {
              uni.showToast({ title: "删除失败", icon: "none" });
            }
          }
        },
      });
    },
    handleSwitchHistory() {
      this.currentTab = 2;
      // 默认查询最近一周
      if (!this.historyParams.start) {
        const d = new Date();
        d.setDate(d.getDate() - 7);
        this.historyParams.start = d.toISOString().split("T")[0];
        this.historyParams.end = this.todayDate;
      }
      this.fetchHistory(true);
    },

    async fetchHistory(isRefresh = false) {
      if (isRefresh) this.historyList = [];
      uni.showLoading({ title: "加载中" });
      try {
        // 调用你补充的 getTaskHistory 接口
        const res = await reminderApi.getTaskHistory(this.historyParams);
        // 处理响应 (根据你的 api.js，直接返回 res 或 res.data)
        const data = res.data || res;
        this.historyList = isRefresh ? data : [...this.historyList, ...data];
      } catch (e) {
        uni.showToast({ title: "加载失败", icon: "none" });
      } finally {
        uni.hideLoading();
      }
    },

    formatTime(isoString) {
      if (!isoString) return "";
      return isoString.replace("T", " ").substring(0, 16);
    },
  },
};
</script>

<style lang="scss" scoped>
/* 配色变量 - 提取自图片 */
$bg-color: #f8f9fd;
$text-dark: #1d1d2b;
$text-grey: #9fa0ab;
$primary-purple: #9f9af8;
/* 按钮/高亮色 */
$card-purple-bg: #ecebff;
/* 任务卡片背景 */
$white: #ffffff;
$border-radius: 30rpx;

page {
  background-color: $bg-color;
  font-family:
    -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial,
    sans-serif;
}

.container {
  min-height: 100vh;
  background-color: $bg-color;
  display: flex;
  flex-direction: column;
}

/* 顶部导航 */
.custom-navbar {
  background: $white;
  padding: 100rpx 40rpx 20rpx;
  /* 适配全面屏顶部 */
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.02);

  .nav-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30rpx;

    .nav-back {
      width: 90rpx;
      height: 90rpx;

      border-radius: 45rpx;

      background-color: #f3f3f3;

      display: flex;
      align-items: center;
      justify-content: center;

      .back-icon {
        width: 40rpx;
        height: 40rpx;
        background-image: url("/static/Register/back.png");
        background-size: cover;
        background-position: center;
      }
    }

    .date-badge {
      background: #f2f2f2;
      padding: 10rpx 24rpx;
      border-radius: 20rpx;
      font-size: 24rpx;
      font-weight: 600;
      color: $text-dark;
    }
  }

  /* 胶囊Tab切换 */
  .tab-pill-container {
    display: flex;
    background: #f5f5f7;
    border-radius: 20rpx;
    padding: 8rpx;

    .tab-pill {
      flex: 1;
      text-align: center;
      padding: 16rpx 0;
      border-radius: 16rpx;
      font-size: 28rpx;
      color: $text-grey;
      font-weight: 600;
      transition: all 0.3s;

      &.active {
        background: $white;
        color: $text-dark;
        box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.05);
      }
    }
  }
}

.content-area {
  flex: 1;
  padding: 0 40rpx;
  box-sizing: border-box;
}

/* 日历条样式 */
.calendar-strip {
  margin-top: 40rpx;

  .calendar-scroll {
    white-space: nowrap;
    width: 100%;
  }

  .calendar-row {
    display: flex;
    padding-bottom: 10rpx;
  }

  .date-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100rpx;
    height: 130rpx;
    margin-right: 20rpx;
    border-radius: 50rpx;
    background: $white;
    border: 1px solid transparent;
    transition: all 0.3s;

    .week-text {
      font-size: 24rpx;
      color: $text-grey;
      margin-bottom: 8rpx;
    }

    .day-circle {
      width: 70rpx;
      height: 70rpx;
      border-radius: 35rpx;
      background: #f3f3f3;
      display: flex;
      align-items: center;
      justify-content: center;

      &.date-active {
        background: $card-purple-bg;
        border-color: $primary-purple;

        .week-text {
          color: $primary-purple;
        }

        .day-text {
          color: $primary-purple;
        }
      }
    }

    .day-text {
      font-size: 16rpx;
      font-weight: bold;
      color: $text-dark;
    }
  }
}

/* 标题区 */
.section-header {
  display: flex;
  align-items: center;
  margin: 40rpx 0 30rpx;

  .section-title {
    font-size: 36rpx;
    font-weight: bold;
    color: $text-dark;
  }

  .badge-count {
    background: #ff4d4f;
    color: $white;
    font-size: 20rpx;
    padding: 4rpx 12rpx;
    border-radius: 20rpx;
    margin-left: 16rpx;
  }
}

/* 时间轴任务列表 */
.timeline-container {
  position: relative;

  /* 时间轴竖线 (简单模拟) */
  &::before {
    content: "";
    position: absolute;
    left: 80rpx;
    top: 20rpx;
    bottom: 20rpx;
    width: 2rpx;
    background: #e0e0e0;
    z-index: 0;
  }

  .timeline-row {
    display: flex;
    margin-bottom: 40rpx;
    position: relative;
    z-index: 1;

    .time-column {
      width: 80rpx;
      text-align: left;
      padding-top: 10rpx;

      .time-text {
        font-size: 26rpx;
        font-weight: 600;
        color: $text-dark;
        background: $bg-color;
        padding-right: 10rpx;
      }
    }

    .card-column {
      flex: 1;
      margin-left: 20rpx;
      padding: 30rpx;
      border-radius: $border-radius;
      transition: all 0.2s;

      /* 两种状态的卡片样式 */
      &.card-active {
        background: $card-purple-bg;

        .card-title {
          color: $text-dark;
        }
      }

      &.card-done {
        background: $white;
        opacity: 0.8;

        .card-title {
          text-decoration: line-through;
          color: $text-grey;
        }
      }

      .card-title {
        font-size: 32rpx;
        font-weight: bold;
        margin-bottom: 8rpx;
        display: block;
      }

      .card-desc {
        font-size: 26rpx;
        color: #7a7a85;
        margin-bottom: 24rpx;
        display: block;
      }

      .card-footer {
        display: flex;
        justify-content: flex-end;

        .status-pill {
          font-size: 24rpx;
          font-weight: bold;
          padding: 8rpx 20rpx;
          border-radius: 30rpx;

          &.pending {
            background: $white;
            color: $text-dark;
          }

          background: #e8f5e9;
          color: #4caf50;
          /* Done style */
        }
      }
    }
  }
}

.icon {
  width: 48rpx;
  height: 48rpx;
  color: #94a3b8;
}

/* 计划列表卡片 (Tab 2) */
.plan-list-container {
  padding-top: 30rpx;
}

.plan-card-modern {
  background: $white;
  border-radius: 30rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);

  .plan-top {
    display: flex;
    align-items: flex-start;
    margin-bottom: 20rpx;

    .plan-icon {
      font-size: 40rpx;
      margin-right: 20rpx;
      background: #f2f2f7;
      width: 80rpx;
      height: 80rpx;
      text-align: center;
      line-height: 80rpx;
      border-radius: 20rpx;
    }

    .plan-info {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .p-name {
      font-size: 32rpx;
      font-weight: bold;
      color: $text-dark;
    }

    .p-dosage {
      font-size: 24rpx;
      color: $text-grey;
      margin-top: 6rpx;
    }

    .plan-opt {
      .opt-btn {
        font-size: 36rpx;
        padding: 10rpx;
        color: $text-grey;

        &.delete {
          color: #ff6b6b;
        }
      }
    }
  }

  .plan-times {
    display: flex;
    flex-wrap: wrap;
    margin-bottom: 16rpx;
  }

  .time-chip {
    background: #f0f0f5;
    color: $text-dark;
    font-size: 22rpx;
    padding: 6rpx 16rpx;
    border-radius: 12rpx;
    margin-right: 12rpx;
    margin-bottom: 10rpx;
    font-weight: 600;
  }

  .plan-dates {
    font-size: 22rpx;
    color: #aaa;
  }
}

/* 悬浮按钮 */
.fab-add-btn {
  position: fixed;
  bottom: 60rpx;
  right: 40rpx;
  width: 110rpx;
  height: 110rpx;
  background: $text-dark;
  border-radius: 40rpx;
  color: $white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.2);

  .plus-icon {
    font-size: 60rpx;
    font-weight: 300;
    margin-top: -10rpx;
  }
}

/* 弹窗现代化 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-content-modern {
  width: 80%;
  background: $white;
  border-radius: 40rpx;
  padding: 50rpx 40rpx;

  .m-title {
    font-size: 36rpx;
    font-weight: 800;
    color: $text-dark;
    display: block;
    text-align: center;
    margin-bottom: 40rpx;
  }

  .input-modern {
    background: #f7f8fa;
    height: 90rpx;
    border-radius: 24rpx;
    padding: 0 30rpx;
    margin-bottom: 24rpx;
    font-size: 28rpx;
    color: $text-dark;
  }

  .picker-row {
    display: flex;
    gap: 20rpx;
    margin-bottom: 24rpx;
  }

  .picker-btn {
    background: #f7f8fa;
    padding: 24rpx;
    border-radius: 24rpx;
    font-size: 26rpx;
    color: #666;
    text-align: center;
    min-width: 160rpx;
  }

  .time-config {
    margin-bottom: 30rpx;

    .label {
      font-size: 26rpx;
      color: $text-grey;
      margin-bottom: 16rpx;
      display: block;
    }

    .tags-wrapper {
      display: flex;
      flex-wrap: wrap;
      gap: 16rpx;
    }

    .t-tag {
      background: $card-purple-bg;
      color: $primary-purple;
      padding: 10rpx 24rpx;
      border-radius: 20rpx;
      font-size: 24rpx;
      font-weight: bold;
    }

    .add-tag-btn {
      border: 2rpx dashed #ddd;
      color: #aaa;
      padding: 8rpx 20rpx;
      border-radius: 20rpx;
      font-size: 24rpx;
    }
  }

  .modal-actions {
    display: flex;
    gap: 20rpx;
    margin-top: 40rpx;

    button {
      flex: 1;
      height: 90rpx;
      line-height: 90rpx;
      border-radius: 24rpx;
      font-size: 30rpx;
      font-weight: bold;
      border: none;

      &:after {
        border: none;
      }
    }

    .btn-cancel {
      background: #f2f2f7;
      color: $text-grey;
    }

    .btn-save {
      background: $text-dark;
      color: $white;
    }
  }
}

.empty-tip {
  text-align: center;
  color: #ccc;
  margin-top: 60rpx;
  font-size: 28rpx;
}

/* --- 历史记录专属样式 --- */
.history-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 20rpx 30rpx;
  border-radius: 20rpx;
  margin: 30rpx 0;

  .filter-item {
    font-size: 26rpx;
    color: $text-dark;
    background: #f5f5f7;
    padding: 10rpx 24rpx;
    border-radius: 12rpx;
  }

  .filter-sep {
    color: $text-grey;
  }
}

.history-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.02);

  .h-main {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .h-info {
    .h-name {
      font-size: 30rpx;
      font-weight: bold;
      color: $text-dark;
      display: block;
    }

    .h-time {
      font-size: 24rpx;
      color: $text-grey;
      margin-top: 8rpx;
      display: block;
    }
  }

  .h-status {
    font-size: 24rpx;
    padding: 6rpx 20rpx;
    border-radius: 12rpx;

    &.st-done {
      background: #e8f5e9;
      color: #4caf50;
    }

    &.st-miss {
      background: #ffebee;
      color: #ef5350;
    }
  }

  .h-footer {
    margin-top: 20rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid #f5f5f5;
    font-size: 22rpx;
    color: #ccc;
  }
}

/* 之前卡片的微调 */
.card-column.card-active {
  background: $card-purple-bg;
}

.card-column.card-done {
  background: #fff;
  opacity: 0.6;
}

.status-pill.pending {
  background: #fff !important;
  color: $text-dark !important;
}

/* 搜索框外部容器 */
.nav-search {
  padding: 20rpx 0;
  background-color: transparent;
  /* 保持与页面背景融合 */

  .search-inner {
    display: flex;
    align-items: center;
    background-color: #f2f2f2;
    /* 白色背景 */
    height: 90rpx;
    border-radius: 45rpx;
    /* 大圆角 */
    padding: 0 30rpx;
    box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
    /* 极淡的阴影 */
    border: 1rpx solid #f0f0f5;
    /* 极细的边框线 */

    .search-icon {
      margin-right: 20rpx;
    }

    input {
      flex: 1;
      height: 100%;
      font-size: 28rpx;
      color: #1d1d2b;
    }

    .search-placeholder {
      color: #9fa0ab;
    }

    .clear-btn {
      font-size: 40rpx;
      color: #ccc;
      padding: 0 10rpx;
      font-weight: 300;
    }
  }
}
</style>
