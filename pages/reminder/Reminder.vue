<template>
  <view class="container">
    <view class="tabs">
      <view
        class="tab-item"
        :class="{ active: currentTab === 0 }"
        @click="currentTab = 0"
        >今日任务</view
      >
      <view
        class="tab-item"
        :class="{ active: currentTab === 1 }"
        @click="currentTab = 1"
        >用药计划</view
      >
    </view>

    <scroll-view scroll-y class="content-area" v-if="currentTab === 0">
      <view class="header-date">
        <text class="date-text">{{ todayDate }}</text>
        <text class="sub-text">按时服药，早日康复</text>
      </view>

      <view v-if="taskList.length === 0" class="empty-tip"
        >今日暂无用药任务</view
      >

      <view
        class="task-card"
        v-for="(task, index) in taskList"
        :key="task.id"
        :class="{ 'task-done': task.status === 1 }"
        @click="handleTaskClick(task, index)"
      >
        <view class="time-col">
          <text class="time">{{ task.timePoint }}</text>
          <text class="status-dot"></text>
        </view>
        <view class="info-col">
          <text class="med-name">{{ task.medicineName }}</text>
          <text class="med-dosage">剂量: {{ task.dosage }}</text>
        </view>
        <view class="action-col">
          <view class="check-btn" v-if="task.status === 1">✓</view>
          <view class="uncheck-btn" v-else>服用</view>
        </view>
      </view>
    </scroll-view>

    <scroll-view scroll-y class="content-area" v-if="currentTab === 1">
      <view class="plan-list">
        <view
          class="plan-card"
          v-for="(plan, index) in planList"
          :key="plan.id"
        >
          <view class="plan-header">
            <text class="plan-name">{{ plan.medicineName }}</text>
            <view class="plan-actions">
              <text class="btn-edit" @click="openPlanModal('edit', plan)"
                >编辑</text
              >
              <text class="btn-del" @click="handleDeletePlan(plan.id, index)"
                >删除</text
              >
            </view>
          </view>
          <view class="plan-detail">
            <view class="tag">每次 {{ plan.dosage }}</view>
            <view
              class="tag-outline"
              v-for="(t, ti) in plan.timePoints"
              :key="ti"
              >{{ t }}</view
            >
          </view>
          <view class="plan-date">
            {{ plan.startDate }} 至 {{ plan.endDate || "长期" }}
          </view>
          <view class="plan-remark" v-if="plan.remark"
            >备注: {{ plan.remark }}</view
          >
        </view>
      </view>

      <view class="fab-add" @click="openPlanModal('add')">+</view>
    </scroll-view>

    <view class="modal-mask" v-if="showModal">
      <view class="modal-content">
        <view class="modal-title">{{
          modalType === "add" ? "新增计划" : "编辑计划"
        }}</view>

        <input
          class="input-field"
          v-model="formData.medicineName"
          placeholder="药品名称 (如: 阿莫西林)"
        />
        <input
          class="input-field"
          v-model="formData.dosage"
          placeholder="剂量 (如: 2粒)"
        />

        <view class="date-picker-row">
          <picker
            mode="date"
            :value="formData.startDate"
            @change="(e) => (formData.startDate = e.detail.value)"
          >
            <view class="picker-box"
              >开始: {{ formData.startDate || "请选择" }}</view
            >
          </picker>
          <picker
            mode="date"
            :value="formData.endDate"
            @change="(e) => (formData.endDate = e.detail.value)"
          >
            <view class="picker-box"
              >结束: {{ formData.endDate || "可选" }}</view
            >
          </picker>
        </view>

        <view class="time-section">
          <text class="section-label">服药时间点:</text>
          <view class="time-tags">
            <view
              class="time-tag"
              v-for="(t, i) in formData.timePoints"
              :key="i"
              @click="removeTimePoint(i)"
            >
              {{ t }} ×
            </view>
            <picker mode="time" @change="addTimePoint">
              <view class="add-time-btn">+ 添加时间</view>
            </picker>
          </view>
        </view>

        <input
          class="input-field"
          v-model="formData.remark"
          placeholder="备注 (如: 饭后服用)"
        />

        <view class="modal-btns">
          <button class="btn-cancel" @click="showModal = false">取消</button>
          <button class="btn-confirm" @click="submitPlan">保存</button>
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
      taskList: [],
      planList: [],

      // 弹窗相关
      showModal: false,
      modalType: "add", // 'add' or 'edit'
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
    };
  },
  onLoad() {
    // 首次加载，获取数据并缓存 [cite: 147]
    this.initData();
  },
  methods: {
    getUserId() {
      console.log("获取用户ID");
      const userId = uni.getStorageSync("userId");
      this.userId = userId;
      console.log("当前用户ID:", this.userId);
    },
    async initData() {
      this.getUserId();
      await this.fetchTasks();
      await this.fetchPlans();
    },

    // --- 任务逻辑 ---
    async fetchTasks() {
      try {
        const res = await reminderApi.getTodayTasks(this.userId);
        console.log("获取今日任务响应:", res);
        // 对时间点进行排序，体验更好
        this.taskList = res.sort((a, b) =>
          a.timePoint.localeCompare(b.timePoint),
        );
      } catch (e) {
        console.error("获取任务失败", e);
      }
    },

    async handleTaskClick(task, index) {
      if (task.status === 1) return; // 已经服用则不处理（根据需求可改为取消服用）

      try {
        // 调用接口修改状态 [cite: 106]
        const res = await reminderApi.updateTaskStatus(task.id, this.userId, 1);
        // 前端直接修改缓存数组，不刷新列表 [cite: 147]
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
        // 处理JSON字符串转数组（如果后端返回的是JSON字符串）
        this.planList = res.map((item) => {
          // 确保timePoints是数组
          if (typeof item.timePoints === "string") {
            try {
              item.timePoints = JSON.parse(item.timePoints);
            } catch (e) {}
          }
          return item;
        });
        console.log("获取用药计划响应:", JSON.parse(JSON.stringify(this.planList)));
      } catch (e) {
        console.error(e);
      }
    },

    // 打开弹窗
    openPlanModal(type, plan = null) {
      this.modalType = type;
      if (type === "edit" && plan) {
        // 深拷贝防止直接修改列表显示
        console.log("编辑计划数据:", JSON.parse(JSON.stringify(this.planList)));
        this.formData = JSON.parse(JSON.stringify(plan));
      } else {
        // 重置表单
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

    // 提交计划 (新增或编辑)
    async submitPlan() {
      // 简单校验
      if (
        !this.formData.medicineName ||
        this.formData.timePoints.length === 0
      ) {
        return uni.showToast({ title: "请填写完整信息", icon: "none" });
      }

      try {
        if (this.modalType === "add") {
          // 新增逻辑 [cite: 34]
          const res = await reminderApi.addPlan(this.userId, this.formData);
          // 前端直接添加到缓存数组 [cite: 148]
          this.planList.push(res);

          // 特殊逻辑：如果开始时间是今天，需要刷新今日任务
          if (this.formData.startDate === this.todayDate) {
            this.fetchTasks();
          }
          uni.showToast({ title: "添加成功" });
        } else {
          // 编辑逻辑 [cite: 66]
          // 注意：文档 [cite: 151, 152] 说明编辑不会影响今日已生成的任务，防止篡改历史
          const res = await reminderApi.editPlan(
            this.formData.id,
            this.userId,
            this.formData,
          );
          // 更新本地列表
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

    // 删除计划
    async handleDeletePlan(planId, index) {
      uni.showModal({
        title: "确认删除",
        content: "删除计划将取消后续所有提醒",
        success: async (res) => {
          if (res.confirm) {
            try {
              const apiRes = await reminderApi.deletePlan(planId, this.userId); // [cite: 76]
              // 前端直接移除 [cite: 148]
              this.planList.splice(index, 1);
              // 文档 [cite: 154] 提到删除计划对应删除任务，
              // 前端简单处理：重新拉取今日任务确保准确，或手动过滤掉该计划ID的任务
              this.fetchTasks();
              uni.showToast({ title: "已删除" });
            } catch (e) {
              uni.showToast({ title: "删除失败", icon: "none" });
            }
          }
        },
      });
    },
  },
};
</script>

<style lang="scss" scoped>
/* 颜色变量 */
$primary-color: #4facfe;
$text-main: #333;
$text-light: #999;
$bg-color: #f5f7fa;
$white: #ffffff;

.container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg-color;
}

/* Tab 样式 */
.tabs {
  display: flex;
  background: $white;
  padding: 20rpx 0;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
  .tab-item {
    flex: 1;
    text-align: center;
    font-size: 30rpx;
    color: $text-light;
    position: relative;
    &.active {
      color: $primary-color;
      font-weight: bold;
      &:after {
        content: "";
        position: absolute;
        bottom: -10rpx;
        left: 50%;
        transform: translateX(-50%);
        width: 40rpx;
        height: 4rpx;
        background: $primary-color;
        border-radius: 2rpx;
      }
    }
  }
}

.content-area {
  flex: 1;
  padding: 30rpx;
  box-sizing: border-box;
}

/* 首页：日期头 */
.header-date {
  margin-bottom: 30rpx;
  .date-text {
    font-size: 40rpx;
    font-weight: bold;
    color: $text-main;
    display: block;
  }
  .sub-text {
    font-size: 24rpx;
    color: $text-light;
  }
}

/* 任务卡片 */
.task-card {
  background: $white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.03);
  transition: all 0.3s;

  &.task-done {
    opacity: 0.7;
    background: #fcfcfc;
    .time,
    .med-name {
      color: $text-light;
      text-decoration: line-through;
    }
    .status-dot {
      background: #52c41a;
    }
  }

  .time-col {
    width: 120rpx;
    text-align: center;
    border-right: 1px solid #eee;
    margin-right: 20rpx;
    .time {
      font-size: 32rpx;
      font-weight: bold;
      color: $text-main;
      display: block;
    }
    .status-dot {
      width: 12rpx;
      height: 12rpx;
      border-radius: 50%;
      background: #faad14;
      display: inline-block;
      margin-top: 8rpx;
    }
  }

  .info-col {
    flex: 1;
    .med-name {
      font-size: 30rpx;
      font-weight: bold;
      color: $text-main;
      display: block;
    }
    .med-dosage {
      font-size: 24rpx;
      color: $text-light;
      margin-top: 8rpx;
      display: block;
    }
  }

  .action-col {
    .check-btn {
      width: 60rpx;
      height: 60rpx;
      background: #e6f7ff;
      color: $primary-color;
      border-radius: 50%;
      text-align: center;
      line-height: 60rpx;
      font-weight: bold;
    }
    .uncheck-btn {
      padding: 10rpx 24rpx;
      background: $primary-color;
      color: $white;
      border-radius: 30rpx;
      font-size: 24rpx;
    }
  }
}

/* 计划列表 */
.plan-card {
  background: $white;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  .plan-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16rpx;
    .plan-name {
      font-size: 32rpx;
      font-weight: bold;
    }
    .plan-actions text {
      margin-left: 20rpx;
      font-size: 24rpx;
    }
    .btn-edit {
      color: $primary-color;
    }
    .btn-del {
      color: #ff4d4f;
    }
  }
  .plan-detail {
    display: flex;
    flex-wrap: wrap;
    margin-bottom: 12rpx;
    .tag {
      background: #e6f7ff;
      color: $primary-color;
      font-size: 22rpx;
      padding: 4rpx 12rpx;
      border-radius: 8rpx;
      margin-right: 10rpx;
    }
    .tag-outline {
      border: 1px solid #eee;
      color: $text-light;
      font-size: 22rpx;
      padding: 2rpx 10rpx;
      border-radius: 8rpx;
      margin-right: 10rpx;
    }
  }
  .plan-date,
  .plan-remark {
    font-size: 24rpx;
    color: $text-light;
  }
}

.fab-add {
  position: fixed;
  bottom: 60rpx;
  right: 40rpx;
  width: 100rpx;
  height: 100rpx;
  background: $primary-color;
  border-radius: 50%;
  color: $white;
  font-size: 60rpx;
  text-align: center;
  line-height: 90rpx;
  box-shadow: 0 8rpx 20rpx rgba(79, 172, 254, 0.4);
}

/* 弹窗样式 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  .modal-content {
    width: 80%;
    background: $white;
    border-radius: 20rpx;
    padding: 40rpx;
    .modal-title {
      font-size: 34rpx;
      font-weight: bold;
      text-align: center;
      margin-bottom: 30rpx;
    }
    .input-field {
      background: #f5f7fa;
      height: 80rpx;
      border-radius: 10rpx;
      padding: 0 20rpx;
      margin-bottom: 20rpx;
      font-size: 28rpx;
    }
    .date-picker-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 20rpx;
      .picker-box {
        background: #f5f7fa;
        padding: 16rpx;
        border-radius: 10rpx;
        font-size: 24rpx;
        color: #666;
        width: 240rpx;
        text-align: center;
      }
    }
    .time-section {
      margin-bottom: 20rpx;
      .section-label {
        font-size: 26rpx;
        color: #666;
        margin-bottom: 10rpx;
        display: block;
      }
      .time-tags {
        display: flex;
        flex-wrap: wrap;
        .time-tag {
          background: #fff0f6;
          color: #ff85c0;
          padding: 6rpx 16rpx;
          border-radius: 20rpx;
          font-size: 24rpx;
          margin-right: 16rpx;
          margin-bottom: 10rpx;
        }
        .add-time-btn {
          border: 1px dashed #ccc;
          color: #ccc;
          padding: 4rpx 16rpx;
          border-radius: 20rpx;
          font-size: 24rpx;
        }
      }
    }
    .modal-btns {
      display: flex;
      margin-top: 40rpx;
      button {
        flex: 1;
        font-size: 30rpx;
        height: 80rpx;
        line-height: 80rpx;
      }
      .btn-cancel {
        background: #f5f5f5;
        color: #666;
        margin-right: 20rpx;
      }
      .btn-confirm {
        background: $primary-color;
        color: $white;
      }
    }
  }
}

.empty-tip {
  text-align: center;
  color: #ccc;
  margin-top: 100rpx;
  font-size: 28rpx;
}
</style>
