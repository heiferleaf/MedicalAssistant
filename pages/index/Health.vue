<template>
  <view class="container">
    <view class="status-bar"></view>

    <view class="nav-header" style="flex-direction: column">
      <view class="padding"></view>
      <view
        style="
          display: flex;
          width: 100%;
          justify-content: space-between;
          align-items: center;
        "
      >
        <view class="header-left">
          <text class="material-icons main-icon">medication</text>
          <text class="header-title">我的药箱</text>
        </view>
        <button class="add-btn" @tap="openForm('add')">
          <text class="material-icons add-icon">add</text>
          <text>新增药品</text>
        </button></view
      >
    </view>

    <scroll-view scroll-y class="main-content">
      <view class="content-wrapper">
        <view class="summary-card">
          <view class="summary-info">
            <text class="summary-label">药箱共有</text>
            <text class="summary-count">{{ medicineList.length }} 种药物</text>
            <view class="update-tag">
              <text class="tag-text">最后更新: {{ getLastUpdate() }}</text>
            </view>
          </view>
          <text class="material-icons card-bg-icon">pill</text>
        </view>

        <view class="medicine-list">
          <view
            class="med-card"
            v-for="(item, index) in medicineList"
            :key="index"
          >
            <view class="card-top">
              <view class="med-avatar-box">
                <view
                  class="avatar-inner"
                  :style="{ backgroundColor: getBgColor(item.id) }"
                >
                  <image
                    class="med-svg-icon"
                    :src="
                      '../../static/Medicine/medicine' +
                      ((item.id % 6) + 1) +
                      '.svg'
                    "
                    mode="aspectFit"
                  ></image>
                </view>
                <view class="med-text-info">
                  <text class="med-name">{{ item.name }}</text>
                  <view class="info-row">
                    <text class="material-icons info-icon">scale</text>
                    <text class="info-text"
                      >推荐剂量：{{ item.defaultDosage }}</text
                    >
                  </view>
                  <view class="info-row">
                    <text class="material-icons info-icon">notes</text>
                    <text class="info-text">备注：{{ item.remark }}</text>
                  </view>
                </view>
              </view>
              <view class="card-actions">
                <view class="action-btn edit" @tap="openForm('edit', item)">
                  <text class="material-icons">edit</text>
                </view>
                <view class="action-btn delete" @tap="handleDelete(item.id)">
                  <text class="material-icons">delete</text>
                </view>
              </view>
            </view>
            <button class="plan-btn" @tap="openPlanForm(item)">
              <text class="material-icons btn-icon">event_note</text>
              <text>生成计划</text>
            </button>
          </view>
        </view>
      </view>
    </scroll-view>
    <view class="modal-mask" v-if="showForm">
      <view class="modal-content">
        <text class="modal-title">{{
          formType === "add" ? "新增药品" : "编辑药品"
        }}</text>
        <input
          class="input"
          v-model="formData.name"
          placeholder="请输入药品名称(必填)"
        />
        <input
          class="input"
          v-model="formData.defaultDosage"
          placeholder="推荐剂量 (如: 2粒)"
        />
        <input
          class="input"
          v-model="formData.remark"
          placeholder="备注 (如: 饭后服用)"
        />

        <view class="modal-actions">
          <button size="mini" @click="showForm = false">取消</button>
          <button size="mini" type="primary" @click="submitForm">保存</button>
        </view>
      </view>
    </view>

    <view class="modal-mask" v-if="showPlanForm">
      <view class="modal-content-modern">
        <view class="modal-header">
          <text class="m-title">{{
            modalType === "add" ? "新增计划" : "编辑计划"
          }}</text>
        </view>

        <input
          class="input-modern"
          v-model="planFormData.medicineName"
          placeholder="药品名称"
        />
        <input
          class="input-modern"
          v-model="planFormData.dosage"
          placeholder="剂量 (如: 2粒)"
        />

        <view class="picker-row">
          <picker
            mode="date"
            :value="planFormData.startDate"
            @change="(e) => (planFormData.startDate = e.detail.value)"
          >
            <view class="picker-btn"
              >开始: {{ planFormData.startDate || "选择" }}</view
            >
          </picker>
          <picker
            mode="date"
            :value="planFormData.endDate"
            @change="(e) => (planFormData.endDate = e.detail.value)"
          >
            <view class="picker-btn"
              >结束: {{ planFormData.endDate || "可选" }}</view
            >
          </picker>
        </view>

        <view class="time-config">
          <text class="label">时间点:</text>
          <view class="tags-wrapper">
            <view
              class="t-tag"
              v-for="(t, i) in planFormData.timePoints"
              :key="i"
              @click="removeTimePoint(i)"
              >{{ t }} ×
            </view>
            <picker mode="time" @change="addTimePoint">
              <view class="add-tag-btn">+ 添加</view>
            </picker>
          </view>
        </view>

        <input
          class="input-modern"
          v-model="planFormData.remark"
          placeholder="备注"
        />

        <view class="modal-actions">
          <button class="btn-cancel" @click="showPlanForm = false">取消</button>
          <button class="btn-save" @click="submitPlan">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
// 引入您刚刚封装的 API
// ⚠️ 请确保这里的相对路径指向您项目中的实际位置
import medicineApi from "../../api/medicine.js";

export default {
  data() {
    return {
      globalFontSize: 16, // 默认基准字体大小 16px
      isDarkMode: false,
      medicineList: [],

      showForm: false,
      formType: "add",
      formData: {
        id: null,
        name: "",
        defaultDosage: "",
        remark: "",
      },

      showPlanForm: false,
      currentMedId: null,
      planTimeInput: "",
      planFormData: {
        id: null,
        medicineName: "",
        dosage: "",
        startDate: "",
        endDate: "",
        timePoints: [],
        remark: "",
      },
    };
  },
  // 2. 使用 onShow 替代 mounted，这样从设置页返回时能立即刷新字体
  onShow() {
    this.applyGlobalSettings();
  },
  mounted() {
    this.fetchMedicineList();
    this.applyGlobalSettings();
  },
  methods: {
    // 根据 ID 获取背景颜色
    getBgColor(id) {
      const colors = [
        "#4D88FF", // 经典蓝
        "#2DD4BF", // 薄荷绿
        "#F59E0B", // 活力橙
        "#8B5CF6", // 极客紫
        "#EF4444", // 珊瑚红
        "#10B981", // 翡翠绿
      ];
      // 确保 ID 是数字，如果是字符串则取其长度或哈希
      const index = typeof id === "number" ? id % 6 : id.length % 6;
      return colors[index];
    },
    addTimePoint(e) {
      const time = e.detail.value;
      if (!this.planFormData.timePoints.includes(time)) {
        this.planFormData.timePoints.push(time);
        this.planFormData.timePoints.sort();
      }
    },

    removeTimePoint(index) {
      this.planFormData.timePoints.splice(index, 1);
    },
    getLastUpdate() {
      if (this.medicineList.length === 0) return "暂无数据";

      const lastItem = this.medicineList.reduce((latest, current) => {
        const latestTime = new Date(latest.updatedAt).getTime();
        const currentTime = new Date(current.updatedAt).getTime();
        return currentTime > latestTime ? current : latest;
      });

      // 格式化时间，例如：2026-03-14 09:22:31
      const date = new Date(lastItem.updatedAt);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
        2,
        "0"
      )}-${String(date.getDate()).padStart(2, "0")} ${String(
        date.getHours()
      ).padStart(2, "0")}:${String(date.getMinutes()).padStart(
        2,
        "0"
      )}:${String(date.getSeconds()).padStart(2, "0")}`;
    },
    // 读取全局设置
    applyGlobalSettings() {
      const savedFont = uni.getStorageSync("app_font_size");
      console.log("读取到的全局字体大小:", savedFont); // 调试输出，查看读取到的值
      if (savedFont) {
        this.globalFontSize = savedFont;
      }
      const savedTheme = uni.getStorageSync("app_theme");
      this.isDarkMode = savedTheme === "dark";
    },
    // 1. 获取药品列表
    async fetchMedicineList() {
      try {
        // uni.showLoading({ title: '加载中...' })
        const res = await medicineApi.getMedicineList();
        // 假设您的 httpRequest 返回了完整的响应体 { code, data, message }
        console.log("获取药品列表响应:", res); // 调试输出，查看 API 返回的完整响应
        if (res) {
          this.medicineList = res || [];
        } else {
          //   uni.showToast({ title: res.message || "获取列表失败", icon: "none" });
        }
      } catch (error) {
        // uni.showToast({ title: "网络异常", icon: "none" });
      } finally {
        // uni.hideLoading()
      }
    },

    // 2. 打开新增/编辑弹窗
    openForm(type, item = null) {
      this.formType = type;
      if (type === "edit" && item) {
        // 回显数据
        this.formData = {
          id: item.id,
          name: item.name,
          defaultDosage: item.defaultDosage || "",
          remark: item.remark || "",
        };
      } else {
        // 清空表单
        this.formData = { id: null, name: "", defaultDosage: "", remark: "" };
      }
      this.showForm = true;
    },

    // 3. 提交新增/编辑药品
    async submitForm() {
      if (!this.formData.name) {
        return uni.showToast({ title: "药品名称不能为空", icon: "none" });
      }

      try {
        uni.showLoading({ title: "保存中..." });
        let res;
        const payload = {
          name: this.formData.name,
          defaultDosage: this.formData.defaultDosage,
          remark: this.formData.remark,
        };

        if (this.formType === "add") {
          res = await medicineApi.addMedicine(payload);
        } else {
          res = await medicineApi.editMedicine(this.formData.id, payload);
        }
        console.log("提交表单响应:", res); // 调试输出，查看 API 返回的完整响应

        if (res) {
          uni.showToast({ title: "保存成功", icon: "success" });
          this.showForm = false;
          this.fetchMedicineList(); // 刷新列表
        } else {
          uni.showToast({ title: res.message || "操作失败", icon: "none" });
        }
      } catch (error) {
        console.error("提交表单失败:", error);
        // uni.showToast({ title: "网络异常", icon: "none" });
      } finally {
        uni.hideLoading();
      }
    },

    // 4. 删除药品
    handleDelete(id) {
      uni.showModal({
        title: "危险操作",
        content: "确定要删除这个药品吗？",
        success: async (confirmRes) => {
          if (confirmRes.confirm) {
            try {
              uni.showLoading({ title: "删除中..." });
              const res = await medicineApi.deleteMedicine(id);
              if (res && res.code === 200) {
                uni.showToast({ title: "删除成功", icon: "success" });
                this.fetchMedicineList(); // 重新获取最新列表
              } else {
                // uni.showToast({
                //   title: res.message || "删除失败",
                //   icon: "none",
                // });
              }
            } catch (error) {
              console.error("删除药品失败:", error);

              //   uni.showToast({ title: "删除失败", icon: "none" });
            } finally {
              uni.hideLoading();
            }
          }
        },
      });
    },

    // 5. 打开创建计划弹窗
    openPlanForm(item) {
      // 自动填入今天的日期作为开始日期
      const today = new Date().toISOString().split("T")[0];
      this.planFormData = {
        medicineName: item.name,
        dosage: item.defaultDosage || "",
        startDate: today,
        endDate: "",
        timePoints: ["08:00", "20:00"], // 默认时间点
        remark: item.remark || "",
      };
      this.currentMedId = item.id;
      this.showPlanForm = true;
    },

    // 6. 提交用药计划
    async submitPlan() {
      if (!this.planFormData.startDate) {
        return uni.showToast({ title: "开始日期不能为空", icon: "none" });
      }
      if (!this.planFormData.timePoints) {
        return uni.showToast({ title: "服药时间不能为空", icon: "none" });
      }

      try {
        uni.showLoading({ title: "创建计划中..." });
        const res = await medicineApi.createMedicinePlan(
          this.currentMedId,
          this.planFormData
        );
        console.log("创建计划响应:", res); // 调试输出，查看 API 返回的完整响应

        if (res) {
          uni.showToast({ title: "计划创建成功", icon: "success" });
          this.showPlanForm = false;
        }
      } catch (error) {
        // uni.showToast({ title: "创建失败", icon: "none" });
      } finally {
        uni.hideLoading();
      }
    },
  },
};
</script>
<style lang="scss">
/* 引入 Material Icons (可以通过网络引用或本地字体) */
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
@import url("https://fonts.googleapis.com/css2?family=Material+Icons");

.padding {
  height: 64rpx; /* 顶部留白，适配状态栏 */
}
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f6f8;
  /* 暗色模式适配变量 */
  --primary: #4d88ff;
  --on-primary: #ffffff;
  --bg-card: #ffffff;
  --text-main: #0f172a;
  --text-sub: #475569;
}

.status-bar {
  height: var(--status-bar-height);
  background-color: #ffffff;
}

/* 导航栏 */
.nav-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 32rpx;
  background-color: #ffffff;
  border-bottom: 1rpx solid #f1f3f9;
  height: 160rpx;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12rpx;

    .main-icon {
      color: var(--primary);
      font-size: 48rpx;
    }

    .header-title {
      font-size: 48rpx;
      font-weight: 700;
      color: var(--text-main);
    }
  }

  .add-btn {
    display: flex;
    align-items: center;
    gap: 4rpx;
    background-color: var(--primary);
    color: var(--on-primary);
    padding: 12rpx 28rpx;
    border-radius: 100rpx;
    font-size: 26rpx;
    font-weight: 700;
    line-height: 1;
    margin: 0;

    .add-icon {
      font-size: 28rpx;
    }
  }
}

.main-content {
  flex: 1;
  overflow: hidden;
}

.content-wrapper {
  padding: 32rpx;
  padding-bottom: 180rpx; /* 留出底部导航空间 */
}

/* 统计卡片 */
.summary-card {
  position: relative;
  background-color: var(--primary);
  border-radius: 24rpx;
  padding: 40rpx;
  margin-bottom: 32rpx;
  box-shadow: 0 10rpx 30rpx rgba(77, 136, 255, 0.3);
  overflow: hidden;

  .summary-info {
    position: relative;
    z-index: 2;

    .summary-label {
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.9);
    }

    .summary-count {
      font-size: 60rpx;
      font-weight: 900;
      color: #ffffff;
      display: block;
      margin-top: 8rpx;
    }

    .update-tag {
      margin-top: 24rpx;
      background-color: rgba(255, 255, 255, 0.2);
      display: inline-block;
      padding: 4rpx 16rpx;
      border-radius: 100rpx;

      .tag-text {
        font-size: 20rpx;
        color: #ffffff;
      }
    }
  }

  .card-bg-icon {
    position: absolute;
    right: -40rpx;
    bottom: -40rpx;
    font-size: 240rpx;
    color: #ffffff;
    opacity: 0.1;
    transform: rotate(12deg);
  }
}

/* 药品卡片列表 */
.medicine-list {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.med-card {
  background-color: var(--bg-card);
  border-radius: 24rpx;
  padding: 32rpx;
  border: 1rpx solid #e2e8f0;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);

  .card-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 32rpx;
  }

  .med-avatar-box {
    display: flex;
    gap: 24rpx;

    .avatar-inner {
      width: 120rpx;
      height: 120rpx;
      background-color: #f0f7ff;
      border-radius: 32rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s;

      .med-type-icon {
        font-size: 60rpx;
        color: var(--primary);
      }
    }

    .med-text-info {
      display: flex;
      flex-direction: column;
      gap: 4rpx;

      .med-name {
        font-size: 34rpx;
        font-weight: 700;
        color: var(--text-main);
      }

      .info-row {
        display: flex;
        align-items: center;
        gap: 8rpx;

        .info-icon {
          font-size: 24rpx;
          color: var(--text-sub);
        }

        .info-text {
          font-size: 26rpx;
          color: var(--text-sub);
        }
      }
    }
  }

  .card-actions {
    display: flex;
    gap: 8rpx;

    .action-btn {
      padding: 16rpx;
      border-radius: 100rpx;
      color: #94a3b8;

      &.delete {
        color: #ef4444;
      }
      &:active {
        background-color: #f1f5f9;
      }
    }
  }

  .plan-btn {
    width: 100%;
    background-color: var(--primary);
    color: #ffffff;
    border-radius: 100rpx;
    height: 96rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12rpx;
    font-weight: 700;
    font-size: 30rpx;

    .btn-icon {
      font-size: 36rpx;
    }
    &:active {
      transform: scale(0.98);
      opacity: 0.9;
    }
  }
}

/* 简易弹窗遮罩和内容样式 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-content {
  width: 80%;
  background: #fff;
  border-radius: 10px;
  padding: 20px;
}

.modal-title {
  /* 原来是 18px，比基准大 2px */
  font-size: calc(var(--base-font) + 2px);
  font-weight: bold;
  margin-bottom: 15px;
  display: block;
  text-align: center;
}

.input {
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 15px;
  /* 原来是 14px，比基准小 2px */
  font-size: calc(var(--base-font) - 2px);
  background-color: #fafafa;
}

.modal-actions {
  display: flex;
  justify-content: space-around;
  margin-top: 10px;
}

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
.med-svg-icon {
  width: 60rpx; /* 白色图标占背景的 60% 左右最合适 */
  height: 60rpx;
}
</style>