<template>
  <view class="health-container">
    <view class="padding"></view>
    <view class="status-bar"></view>

    <scroll-view scroll-y class="scroll-body">
      <header class="header-section">
        <view class="top-bar">
          <text class="page-title">健康数据</text>
          <view class="action-btns">
            <view class="icon-btn" @tap="openCalendar">
              <image class="icon" src="/static/Health/calendar.svg" />
            </view>
            <view class="icon-btn" @tap="addData">
              <image class="icon" src="/static/Health/plus-circle.svg" />
            </view>
          </view>
        </view>

        <view class="index-card">
          <view class="card-top">
            <view>
              <text class="card-label">本周健康指数</text>
              <view class="score-row">
                <text class="score-num">{{ healthIndex.score }}</text>
                <text class="score-tag" :style="{ color: healthIndex.color }">
                  {{ healthIndex.level }}
                </text>
              </view>
            </view>
            <view class="trend-icon">
              <image class="icon" :src="healthStatusIcon" />
            </view>
          </view>

          <view class="card-bottom">
            <text class="trend-text">{{ healthTrendText() }}</text>
            <view class="chart-mini">
              <view
                v-for="(bar, index) in getStepBars()"
                :key="index"
                class="bar"
                :style="{
                  height: bar.height,
                  opacity: bar.isToday ? '1' : '0.3',
                  background: bar.isToday ? '#fff' : 'rgba(255,255,255,1)',
                }"
              ></view>
            </view>
          </view>
        </view>
      </header>

      <section class="stats-section">
        <view class="stats-grid">
          <view class="stat-card half">
            <view class="stat-header">
              <view class="icon-tag bg-rose">
                <image class="icon" src="/static/Home/heart.svg" />
              </view>
              <text class="stat-label">心率</text>
            </view>
            <view class="stat-value">
              <text class="val-num">{{ heart_rate }}</text>
              <text class="val-unit">bpm</text>
            </view>
            <text :class="['status-dot', evaluateHeartRate(heart_rate).color]">
              {{ evaluateHeartRate(heart_rate).text }}
            </text>
          </view>

          <view class="stat-card half">
            <view class="stat-header">
              <view class="icon-tag bg-blue">
                <image class="icon" src="/static/Prepare/blood pressure.svg" />
              </view>
              <text class="stat-label">血压</text>
            </view>
            <view class="stat-value">
              <text class="val-num"
                >{{ max_blood_pressure }}/{{ min_blood_pressure }}</text
              >
              <text class="val-unit">mmHg</text>
            </view>
            <text
              :class="[
                'status-dot',
                evaluateBloodPressure(max_blood_pressure, min_blood_pressure)
                  .color,
              ]"
            >
              {{
                evaluateBloodPressure(max_blood_pressure, min_blood_pressure)
                  .text
              }}
            </text>
          </view>

          <view class="stat-card">
            <view class="stat-header">
              <view class="icon-tag bg-cyan">
                <image class="icon" src="/static/Health/blood_oxygen.svg" />
              </view>
              <text class="stat-label">血氧</text>
            </view>
            <view class="stat-value">
              <text class="val-num">{{ computeBloodOxygen() }}</text>
              <text class="val-unit">%</text>
            </view>
            <text
              :class="['status-dot', evaluateOxygen(max_blood_oxygen).color]"
            >
              {{ evaluateOxygen(max_blood_oxygen).text }}
            </text>
          </view>

          <view class="stat-card">
            <view class="stat-header">
              <view class="icon-tag bg-blue">
                <image class="icon" src="/static/Health/pressure.svg" />
              </view>
              <text class="stat-label">压力</text>
            </view>
            <view class="stat-value">
              <text class="val-num">{{ pressure }}</text>
              <text class="val-unit">分</text>
            </view>
            <text :class="['status-dot', evaluatePressure(pressure).color]">
              {{ evaluatePressure(pressure).text }}
            </text>
          </view>
        </view>

        <view class="stat-card full sleep-card">
          <view class="sleep-left">
            <view class="icon-tag-large bg-indigo">
              <image
                class="icon"
                src="/static/Prepare/sleep.svg"
                style="height: 64rpx; width: 64rpx"
              />
            </view>
            <view class="sleep-info">
              <text class="stat-label">昨晚睡眠</text>
              <text class="val-num-md">{{ sleep_duration }}</text>
            </view>
          </view>
          <view class="sleep-right">
            <text class="efficiency">效率 {{ sleep_score }}%</text>
            <view class="progress-bg">
              <view class="progress-fill" style="width: 92%"></view>
            </view>
          </view>
        </view>

        <view class="stat-card full">
          <view class="stat-header space-between">
            <view class="flex-row">
              <view class="icon-tag bg-orange">
                <image
                  class="icon"
                  src="/static/Prepare/walk.svg"
                  style="height: 48rpx; width: 48rpx"
                />
              </view>
              <text class="stat-label">今日步数</text>
            </view>
            <text class="goal-text">目标: {{ stepGoal }}</text>
          </view>
          <view class="step-content">
            <view class="stat-value">
              <text class="val-num-lg">{{ step_count }}</text>
              <text class="val-unit">步</text>
            </view>
            <view class="progress-bg-lg">
              <view
                class="progress-fill-orange"
                :style="{ width: stepCompletion + '%' }"
              ></view>
            </view>
          </view>
        </view>
      </section>

      <view class="safe-bottom-padding"></view>
    </scroll-view>

    <app-navbar :current="2" />
  </view>
</template>

<script>
import oppoHealthManager from "../../utils/oppoHealthManager";
export default {
  data() {
    return {
      // 数据可以在此动态绑定
      full: {},
      fullstr: "",
      heart_rate: "--",
      max_blood_pressure: "--",
      min_blood_pressure: "--",
      sleep_duration: "--",
      sleep_score: "--",
      step_count: "--",
      stepGoal: "--",
      stepCompletion: 50,
      max_blood_oxygen: "--",
      min_blood_oxygen: "--",
      pressure: "--",
      healthIndex: {
        score: "--",
        level: "--",
        color: "#94a3b8",
      },
    };
  },
  mounted() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      // 页面加载时可以初始化数据或调用接口
      await oppoHealthManager.fetchAllAndCache();

      this.fullstr = uni.getStorageSync("OPPO_HEALTH_FULL_DATA");
      this.full = JSON.parse(this.fullstr);

      this.heart_rate = this.full?.HEART_RATE_COUNT[0]?.average || "--";
      this.max_blood_pressure =
        this.full?.BLOOD_PRESSURE_COUNT[0]?.blood_pressure_systolic_max || "--";
      this.min_blood_pressure =
        this.full?.BLOOD_PRESSURE_COUNT[0]?.blood_pressure_diastolic_min ||
        "--";
      this.sleep_duration = this.full?.SLEEP_COUNT[0]?.total
        ? (this.full?.SLEEP_COUNT[0]?.total / 3600).toFixed(1)
        : "--";
      this.sleep_score = this.full?.SLEEP_COUNT[0]?.sleep_score || "--";
      this.step_count = this.full?.STEP_COUNT[0]?.step || "--";
      this.stepGoal = this.full?.STEP_COUNT[0]?.step_goal || "--";

      this.stepCompletion =
        this.stepGoal > 0
          ? ((this.step_count / this.stepGoal) * 100).toFixed(1)
          : 0;
      this.max_blood_oxygen =
        this.full?.BLOOD_OXYGEN_COUNT[0]?.blood_oxygen_max || "--";
      this.min_blood_oxygen =
        this.full?.BLOOD_OXYGEN_COUNT[0]?.blood_oxygen_min || "--";
      this.pressure = this.full?.PRESSURE_COUNT[0]?.average || "--";

      this.computeHealthIndex();
    },
    computeHealthIndex() {
      // 基础分 0
      let score = 0;
      const data = this.full;
      if (!data) return 0;

      // --- 1. 步数评分 (满分 30) ---
      const stepData = data.STEP_COUNT?.[0];
      if (stepData) {
        const steps = parseInt(stepData.step_count || 0);
        const goal = parseInt(stepData.step_goal || 8000);
        // 按完成比例打分，最高 30 分
        score += Math.max(Math.min((steps / goal) * 30, 30), 18);
      }

      // --- 2. 睡眠评分 (满分 30) ---
      const sleepData = data.SLEEP_COUNT?.[0];
      if (sleepData) {
        // 睡眠分由两部分组成：时长占比(15分) + 官方评分占比(15分)
        const durationHours = (sleepData.total || 0) / 3600;
        const sleepScoreOrigin = parseInt(sleepData.sleep_score || 0);

        // 时长分：6-9小时为满分，过少或过多递减
        let durationScore = 0;
        if (durationHours >= 6 && durationHours <= 9) durationScore = 15;
        else {
          durationScore = 9;
        }

        score += Math.max(durationScore, 9);
        score += (sleepScoreOrigin / 100) * 15;
      }

      // --- 3. 心血管状态评分 (满分 30) ---
      // 心率分 (15分)
      const hr = parseFloat(data.HEART_RATE_COUNT?.[0]?.average || 0);
      if (hr >= 60 && hr <= 100) score += 15; // 正常静息心率
      else {
        score += 9;
      }

      // 血压分 (15分)
      const sysMax = parseInt(
        data.BLOOD_PRESSURE_COUNT?.[0]?.blood_pressure_systolic_max || 0
      );
      const diaMin = parseInt(
        data.BLOOD_PRESSURE_COUNT?.[0]?.blood_pressure_diastolic_min || 0
      );
      if (sysMax > 0) {
        // 理想血压收缩压 < 130 且 舒张压 < 85
        if (sysMax <= 130 && diaMin <= 85) score += 15;
        else score += 9;
      }

      // --- 4. 血氧分 (满分 10) ---
      const oxy = parseFloat(
        data.BLOOD_OXYGEN_COUNT?.[0]?.blood_oxygen_min || 0
      );
      if (oxy >= 95) score += 10;
      else if (oxy >= 90) score += 6;

      // 最终得分四舍五入
      const finalScore = Math.round(score);

      // 返回得分和评级
      this.healthIndex = {
        score: finalScore,
        level: finalScore >= 90 ? "优秀" : finalScore >= 75 ? "良好" : "一般",
        color:
          finalScore >= 90
            ? "#4CD964"
            : finalScore >= 75
            ? "#FEB300"
            : "#FF3B30",
      };
    },
    // 动态生成趋势描述文字
    healthTrendText() {
      const level = this.healthIndex?.level || "未知";
      const score = this.healthIndex?.score || 0;

      const tips = {
        优秀: `当前状态极佳 (${score}分)，请继续保持良好的生活习惯！`,
        良好: `身体状况良好 (${score}分)，增加适量运动会让你更棒。`,
        一般: `健康指数偏低 (${score}分)，建议保证充足睡眠并多走动。`,
        未知: "暂无足够数据，请佩戴设备同步数据。",
      };

      return tips[level] || tips["未知"];
    },
    computeBloodOxygen() {
      return (this.min_blood_oxygen / this.max_blood_oxygen) * 100 || "--";
    },
    openCalendar() {
      uni.showToast({ title: "打开日历", icon: "none" });
    },
    addData() {
      uni.showToast({ title: "添加数据", icon: "none" });
    },
    // 1. 心率评估 (正常范围：60-100 bpm)
    evaluateHeartRate(val) {
      const hr = parseFloat(val);
      if (isNaN(hr)) return { text: "● 未知", color: "text-gray" };
      if (hr < 60) return { text: "● 偏慢", color: "text-orange" };
      if (hr <= 100) return { text: "● 正常", color: "text-emerald" };
      return { text: "● 偏快", color: "text-rose" };
    },

    // 2. 血压评估 (理想：收缩压<130 且 舒张压<85)
    evaluateBloodPressure(max, min) {
      const sys = parseInt(max);
      const dia = parseInt(min);
      if (isNaN(sys) || isNaN(dia))
        return { text: "● 未知", color: "text-gray" };
      if (sys < 130 && dia < 85)
        return { text: "● 理想", color: "text-emerald" };
      if (sys < 140 && dia < 90)
        return { text: "● 正常高值", color: "text-orange" };
      return { text: "● 偏高", color: "text-rose" };
    },

    // 3. 血氧评估 (正常：>= 95%)
    evaluateOxygen(val) {
      const oxy = parseFloat(val);
      if (isNaN(oxy)) return { text: "● 未知", color: "text-gray" };
      if (oxy >= 95) return { text: "● 正常", color: "text-emerald" };
      if (oxy >= 90) return { text: "● 偏低", color: "text-orange" };
      return { text: "● 极低", color: "text-rose" };
    },

    // 4. 压力评估 (正常：1-59，中等：60-79，高：80-100)
    evaluatePressure(val) {
      const p = parseInt(val);
      if (isNaN(p)) return { text: "● 未知", color: "text-gray" };
      if (p < 60) return { text: "● 放松", color: "text-emerald" };
      if (p < 80) return { text: "● 中等", color: "text-orange" };
      return { text: "● 偏高", color: "text-rose" };
    },

    getStepBars() {
      const stepData = this.full?.STEP_COUNT || [];
      // 1. 仅取最近的 5 条记录（对应你 HTML 里的 5 个 bar）
      // 如果数据不足 5 条，前面会自动留空或显示默认高度
      const lastFive = stepData.slice(0, 5).reverse();

      // 2. 找出这 5 天中的最大步数，作为 100% 高度的基准
      const maxStep = Math.max(
        ...lastFive.map((item) => parseInt(item.step || 0)),
        1
      );

      // 3. 映射为高度数据（单位 rpx，最大高度建议设为 60rpx 左右）
      const maxHeight = 60;

      return lastFive.map((item, index) => {
        const currentStep = parseInt(item.step || 0);
        // 计算比例高度
        const barHeight = (currentStep / maxStep) * maxHeight;

        return {
          height: Math.max(barHeight, 10) + "rpx", // 设定最小高度 10rpx 避免看不见
          // 最后一个（即今天）高亮显示
          isToday: index === lastFive.length - 1,
        };
      });
    },
    healthStatusIcon() {
      const score = this.healthIndex?.score;

      // 数据未加载或异常时的兜底图标 (可以使用 simple.svg)
      if (score === undefined || score === null || isNaN(score)) {
        return "/static/Health/simple.svg";
      }

      // 1. 优秀 (>= 90 分)
      if (score >= 90) {
        return "/static/Health/bigsmile.svg";
      }

      // 2. 良好 (75-89 分)
      if (score >= 75) {
        return "/static/Health/smile.svg";
      }

      // 3. 一般 (< 75 分)
      return "/static/Health/simple.svg";
    },
  },
};
</script>

<style lang="scss" scoped>
// 基础变量
$primary-color: #6366f1;
$bg-light: #f8fafc;
$bg-dark: #0f172a;

.text-emerald {
  color: #10b981;
} /* 绿色 */
.text-orange {
  color: #f59e0b;
} /* 橙色 */
.text-rose {
  color: #f43f5e;
} /* 红色 */
.text-gray {
  color: #94a3b8;
} /* 灰色 */

.padding {
  height: 64rpx;
}
.health-container {
  min-height: 100vh;
  background-color: $bg-light;
  @media (prefers-color-scheme: dark) {
    background-color: $bg-dark;
  }
}

.status-bar {
  height: var(--status-bar-height);
  width: 100%;
}

.scroll-body {
  height: calc(100vh - var(--status-bar-height));
}

.header-section {
  padding: 40rpx 40rpx 20rpx;

  .top-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 50rpx;

    .page-title {
      font-size: 48rpx;
      font-weight: 700;
      color: #1e293b;
      @media (prefers-color-scheme: dark) {
        color: #fff;
      }
    }

    .action-btns {
      display: flex;
      gap: 24rpx;

      .icon-btn {
        width: 80rpx;
        height: 80rpx;
        background: #fff;
        border-radius: 24rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
        @media (prefers-color-scheme: dark) {
          background: #1e293b;
        }

        .material-icons {
          font-size: 40rpx;
          color: #64748b;
          @media (prefers-color-scheme: dark) {
            color: #94a3b8;
          }
        }
      }
    }
  }

  .index-card {
    background: $primary-color;
    padding: 50rpx;
    border-radius: 60rpx;
    color: #fff;
    box-shadow: 0 20rpx 40rpx rgba(99, 102, 241, 0.3);

    .card-top {
      display: flex;
      justify-content: space-between;
      margin-bottom: 30rpx;

      .card-label {
        font-size: 28rpx;
        color: rgba(255, 255, 255, 0.8);
        margin-bottom: 10rpx;
        display: block;
      }

      .score-row {
        display: flex;
        align-items: baseline;
        .score-num {
          font-size: 80rpx;
          font-weight: 700;
        }
        .score-tag {
          font-size: 32rpx;
          margin-left: 12rpx;
          font-weight: 500;
        }
      }

      .trend-icon {
        background: transparent;
        padding: 16rpx;
        border-radius: 24rpx;
        height: 48rpx;
      }
    }

    .card-bottom {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;

      .trend-text {
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.8);
      }

      .chart-mini {
        display: flex;
        gap: 8rpx;
        align-items: flex-end;
        .bar {
          width: 12rpx;
          border-radius: 10rpx;
          background: rgba(255, 255, 255, 0.4);
        }
      }
    }
  }
}

.stats-section {
  padding: 0 40rpx;
  display: flex;
  flex-direction: column;
  gap: 30rpx;

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 30rpx;
  }

  .stat-card {
    background: #fff;
    padding: 40rpx;
    border-radius: 48rpx;
    border: 1rpx solid #f1f5f9;
    box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.02);
    @media (prefers-color-scheme: dark) {
      background: #1e293b;
      border-color: rgba(255, 255, 255, 0.05);
    }

    .stat-header {
      display: flex;
      align-items: center;
      gap: 16rpx;
      margin-bottom: 24rpx;
      &.space-between {
        justify-content: space-between;
      }
      .flex-row {
        display: flex;
        align-items: center;
        gap: 16rpx;
      }
    }

    .icon-tag {
      width: 64rpx;
      height: 64rpx;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      .material-icons {
        font-size: 36rpx;
      }
    }

    .stat-label {
      font-size: 28rpx;
      color: #64748b;
      font-weight: 500;
    }

    .stat-value {
      display: flex;
      align-items: baseline;
      gap: 8rpx;
      .val-num {
        font-size: 48rpx;
        font-weight: 700;
        color: #1e293b;
        @media (prefers-color-scheme: dark) {
          color: #fff;
        }
      }
      .val-num-md {
        font-size: 40rpx;
        font-weight: 700;
        color: #1e293b;
        @media (prefers-color-scheme: dark) {
          color: #fff;
        }
      }
      .val-num-lg {
        font-size: 64rpx;
        font-weight: 700;
        color: #1e293b;
        @media (prefers-color-scheme: dark) {
          color: #fff;
        }
      }
      .val-unit {
        font-size: 24rpx;
        color: #94a3b8;
      }
    }

    .status-dot {
      font-size: 24rpx;
      margin-top: 20rpx;
      display: block;
      font-weight: 600;
    }
  }
}

// 颜色辅助类
.bg-rose {
  background: #fff1f2;
  @media (prefers-color-scheme: dark) {
    background: rgba(225, 29, 72, 0.1);
  }
}
.text-rose {
  color: #f43f5e;
}
.bg-blue {
  background: #eff6ff;
  @media (prefers-color-scheme: dark) {
    background: rgba(59, 130, 246, 0.1);
  }
}
.bg-cyan {
  background: #ecfeff;
  @media (prefers-color-scheme: dark) {
    background: rgba(6, 182, 212, 0.1);
  }
}
.text-blue {
  color: #3b82f6;
}
.bg-indigo {
  background: #0037eb;
  @media (prefers-color-scheme: dark) {
    background: rgba(9, 13, 225, 0.1);
  }
}
.text-indigo {
  color: #6366f1;
}
.bg-orange {
  background: #fff7ed;
  @media (prefers-color-scheme: dark) {
    background: rgba(249, 115, 22, 0.1);
  }
}
.text-orange {
  color: #f97316;
}
.text-emerald {
  color: #10b981;
}

.sleep-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .sleep-left {
    display: flex;
    align-items: center;
    gap: 30rpx;
  }
  .icon-tag-large {
    width: 96rpx;
    height: 96rpx;
    border-radius: 32rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    .material-icons {
      font-size: 48rpx;
    }
  }
  .sleep-right {
    text-align: right;
  }
  .efficiency {
    font-size: 24rpx;
    color: $primary-color;
    font-weight: 600;
  }
}

.progress-bg {
  width: 160rpx;
  height: 12rpx;
  background: #f1f5f9;
  border-radius: 10rpx;
  margin-top: 10rpx;
  overflow: hidden;
  @media (prefers-color-scheme: dark) {
    background: #334155;
  }
  .progress-fill {
    height: 100%;
    background: $primary-color;
  }
}

.progress-bg-lg {
  width: 100%;
  height: 24rpx;
  background: #f1f5f9;
  border-radius: 20rpx;
  margin-top: 20rpx;
  overflow: hidden;
  @media (prefers-color-scheme: dark) {
    background: #334155;
  }
  .progress-fill-orange {
    height: 100%;
    background: #fb923c;
    border-radius: 20rpx;
  }
}

.goal-text {
  font-size: 24rpx;
  color: #94a3b8;
}
.safe-bottom-padding {
  height: 240rpx;
}

// 图标字体库
.material-icons {
  font-family: "Material Icons Round";
}
</style>