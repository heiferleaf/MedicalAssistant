<template>
  <view class="page-container">
    <view class="nav-header">
      <view class="status-bar"></view>
      <view class="nav-content">
        <text class="page-title">我的药箱</text>
        <view class="view-switcher" @click="switchView">
          <image 
            class="switch-icon" 
            :src="viewMode === 'list' ? '../../static/Health/calendar.svg' : '../../static/Health/list.svg'" 
            mode="aspectFit" 
          />
          <text>{{ viewMode === "list" ? "日历视图" : "列表视图" }}</text>
        </view>
      </view>
    </view>

    <view class="search-section">
      <view class="nav-search">
        <view class="search-inner">
          <icon type="search" size="18" color="#9FA0AB" class="search-icon" />
          <input 
            type="text" 
            v-model="searchKeyword" 
            placeholder="搜索我的药品名称..." 
            placeholder-class="search-placeholder"
            @confirm="onSearch"
            confirm-type="search"
          />
          <text v-if="searchKeyword" class="clear-btn" @click="clearSearch">×</text>
        </view>
      </view>
    </view>

    <scroll-view class="main-scroll" scroll-y>
      
      <view class="calendar-panel" v-if="viewMode === 'calendar'">
        <view class="calendar-card">
          <text class="c-title">2026年1月</text>
          <view class="c-placeholder">日历视图开发中...</view>
        </view>
      </view>

      <view class="section-box">
        <view class="section-head">
          <text class="s-title">正在服用</text>
          <text class="s-badge">{{ activeMedications.length }}</text>
        </view>

        <view class="med-list">
          <view 
            class="med-card" 
            v-for="med in activeMedications" 
            :key="med.id"
            @click="toMedicationDetail(med.id)"
          >
            <view class="card-top">
              <view class="med-icon-bg">
                <image src="../../static/Health/pill-active.svg" class="pill-img" />
              </view>
              <view class="med-main">
                <text class="med-name">{{ med.name }}</text>
                <view class="med-tags">
                  <text class="m-tag" v-for="tag in med.tags" :key="tag">{{ tag }}</text>
                </view>
              </view>
              <view class="status-tag" :class="med.statusClass">
                <text>{{ med.statusText }}</text>
              </view>
            </view>

            <view class="card-info">
              <view class="info-line">
                <image src="../../static/Health/time.svg" class="i-icon" />
                <text>{{ med.schedule }}</text>
              </view>
              <view class="info-line">
                <image src="../../static/Health/calendar-check.svg" class="i-icon" />
                <text>已服 {{ med.takenDays }} 天 · 剩余 {{ med.remainingDays }} 天</text>
                <text v-if="med.missedCount > 0" class="missed-warn">! 漏服{{ med.missedCount }}次</text>
              </view>
            </view>

            <view class="card-btns">
              <view class="btn-sub" @click.stop="setReminder(med.id)">提醒</view>
              <view class="btn-sub" @click.stop="toMedicationDetail(med.id)">详情</view>
              <view 
                class="btn-main" 
                :class="{ 'is-taken': med.todayTaken }"
                @click.stop="recordDose(med.id)"
              >
                {{ med.todayTaken ? "今日已服" : "记录服用" }}
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="section-box inactive-section">
        <view class="section-head" @click="toggleInactive">
          <text class="s-title">已停用</text>
          <text class="s-badge grey">{{ inactiveMedications.length }}</text>
          <image 
            class="arrow-icon" 
            :class="{rotate: showInactive}" 
            src="../../static/Health/down.svg" 
          />
        </view>

        <view class="med-list" v-if="showInactive">
          <view class="med-card-small" v-for="med in inactiveMedications" :key="med.id">
            <view class="small-info">
              <text class="small-name">{{ med.name }}</text>
              <text class="small-desc">停用于: {{ med.stopTime }} (服用了{{ med.duration }})</text>
            </view>
            <view class="small-btns">
              <text class="btn-text blue" @click="restartMedication(med.id)">重启</text>
              <text class="btn-text red" @click="deleteMedication(med.id)">删除</text>
            </view>
          </view>
        </view>
      </view>
      
      <view class="safe-area-bottom"></view>
    </scroll-view>

    <view class="fab-container">
      <view class="fab-btn" @click="addMedication">
        <image src="../../static/Health/plus-circle.svg" class="fab-icon" />
        <text>添加新计划</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      viewMode: "list",
      searchKeyword: "",
      showInactive: false,
      activeMedications: [
        { id: 1, name: "阿司匹林肠溶片", tags: ["抗血小板", "早饭后"], schedule: "10:00 每次1片", takenDays: 7, remainingDays: 23, missedCount: 0, todayTaken: true, statusClass: "s-normal", statusText: "良好" },
        { id: 2, name: "二甲双胍缓释片", tags: ["降糖药"], schedule: "08:00/18:00 每次1片", takenDays: 3, remainingDays: 27, missedCount: 1, todayTaken: false, statusClass: "s-warn", statusText: "有漏服" }
      ],
      inactiveMedications: [
        { id: 4, name: "头孢克肟片", stopTime: "2025-12-15", duration: "7天" }
      ]
    };
  },
  methods: {
    switchView() { this.viewMode = this.viewMode === 'list' ? 'calendar' : 'list'; },
    clearSearch() { this.searchKeyword = ""; },
    toggleInactive() { this.showInactive = !this.showInactive; },
    toMedicationDetail(id) { uni.navigateTo({ url: `/pages/medication/detail?id=${id}` }); },
    setReminder(id) { uni.showToast({ title: '去设置页面', icon: 'none' }); },
    recordDose(id) {
      const med = this.activeMedications.find(m => m.id === id);
      if (med) med.todayTaken = !med.todayTaken;
    },
    restartMedication(id) { uni.showModal({ title: '提示', content: '是否重新启用该计划？' }); },
    deleteMedication(id) { 
      // 对应规则 3.4：告知用户影响
      uni.showModal({ 
        title: '删除计划', 
        content: '删除后明日起不再提醒，历史记录将保留。',
        confirmColor: '#FF5C5C'
      }); 
    },
    addMedication() { uni.navigateTo({ url: "/pages/medication/add" }); }
  }
};
</script>

<style scoped lang="scss">
/* 全局变量配色 */
$primary: #5C62FF;
$bg-light: #F8F9FD;
$text-main: #1D1D2B;
$text-grey: #9FA0AB;
$white: #FFFFFF;

.page-container {
  min-height: 100vh;
  background-color: $bg-light;
}

.status-bar { height: var(--status-bar-height); }

/* 1. 顶部导航 */
.nav-header {
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  padding: 20rpx 40rpx 40rpx;
  border-radius: 0 0 40rpx 40rpx;
  
  .nav-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 20rpx;
    
    .page-title { font-size: 44rpx; font-weight: 800; color: $white; }
    
    .view-switcher {
      display: flex; align-items: center;
      background: rgba($white, 0.2);
      padding: 12rpx 24rpx;
      border-radius: 30rpx;
      font-size: 24rpx; color: $white;
      .switch-icon { width: 30rpx; height: 30rpx; margin-right: 10rpx; }
    }
  }
}

/* 2. 搜索栏 (完全匹配你要求的样式) */
.search-section {
  padding: 0 40rpx;
  margin-top: -40rpx; /* 向上偏移压在Header上 */
  
  .nav-search {
    .search-inner {
      display: flex; align-items: center;
      background-color: $white;
      height: 90rpx; border-radius: 24rpx;
      padding: 0 30rpx;
      box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.05);
      border: 1rpx solid rgba($primary, 0.05);
      
      input { flex: 1; font-size: 28rpx; color: $text-main; margin-left: 20rpx; }
      .search-placeholder { color: $text-grey; }
      .clear-btn { font-size: 40rpx; color: #CCC; padding-left: 10rpx; }
    }
  }
}

/* 3. 内容滚动区 */
.main-scroll {
  height: calc(100vh - 280rpx);
  padding: 40rpx;
  box-sizing: border-box;
}

.section-box {
  margin-bottom: 50rpx;
  
  .section-head {
    display: flex; align-items: center; margin-bottom: 24rpx;
    .s-title { font-size: 34rpx; font-weight: bold; color: $text-main; }
    .s-badge { 
      background: rgba($primary, 0.1); color: $primary;
      font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; margin-left: 12rpx;
      &.grey { background: #EEE; color: #999; }
    }
    .arrow-icon { width: 24rpx; height: 24rpx; margin-left: auto; transition: 0.3s; }
    .rotate { transform: rotate(180deg); }
  }
}

/* 4. 药品卡片 (Active) */
.med-card {
  background: $white;
  border-radius: 32rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.02);
  border: 1rpx solid rgba($primary, 0.05);

  .card-top {
    display: flex; align-items: flex-start;
    .med-icon-bg {
      width: 90rpx; height: 90rpx; border-radius: 24rpx;
      background: rgba($primary, 0.05);
      display: flex; align-items: center; justify-content: center;
      .pill-img { width: 50rpx; height: 50rpx; }
    }
    .med-main {
      flex: 1; margin-left: 24rpx;
      .med-name { font-size: 34rpx; font-weight: bold; color: $text-main; display: block; }
      .med-tags {
        display: flex; gap: 10rpx; margin-top: 10rpx;
        .m-tag { font-size: 20rpx; color: $text-grey; background: #F3F4F6; padding: 4rpx 12rpx; border-radius: 8rpx; }
      }
    }
    .status-tag {
      font-size: 22rpx; padding: 6rpx 20rpx; border-radius: 12rpx;
      &.s-normal { background: #E6F9F3; color: #00C897; }
      &.s-warn { background: #FFF5F0; color: #FF9F43; }
    }
  }

  .card-info {
    margin: 30rpx 0; border-top: 1rpx solid #F5F5F5; padding-top: 24rpx;
    .info-line {
      display: flex; align-items: center; margin-bottom: 12rpx;
      font-size: 26rpx; color: #666;
      .i-icon { width: 28rpx; height: 28rpx; margin-right: 12rpx; opacity: 0.5; }
      .missed-warn { color: #FF5C5C; font-weight: bold; margin-left: 20rpx; }
    }
  }

  .card-btns {
    display: flex; gap: 20rpx;
    .btn-sub {
      flex: 1; height: 70rpx; border-radius: 18rpx;
      background: #F3F4FF; color: $primary;
      display: flex; align-items: center; justify-content: center; font-size: 26rpx;
    }
    .btn-main {
      flex: 2; height: 70rpx; border-radius: 18rpx;
      background: $primary; color: $white;
      display: flex; align-items: center; justify-content: center; font-size: 26rpx; font-weight: bold;
      &.is-taken { background: #00C897; opacity: 0.8; }
    }
  }
}

/* 5. 停用卡片 (Inactive) */
.med-card-small {
  background: #F3F4F6; padding: 24rpx; border-radius: 24rpx;
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16rpx;
  .small-info {
    .small-name { font-size: 28rpx; font-weight: bold; color: #666; display: block; }
    .small-desc { font-size: 22rpx; color: #999; }
  }
  .small-btns {
    display: flex; gap: 20rpx;
    .btn-text {
      font-size: 24rpx; font-weight: bold;
      &.blue { color: $primary; }
      &.red { color: #FF5C5C; }
    }
  }
}

/* 6. 底部悬浮按钮 */
.fab-container {
  position: fixed; bottom: 120rpx; left: 0; right: 0;
  display: flex; justify-content: center;
  .fab-btn {
    background: $primary; width: 340rpx; height: 90rpx;
    border-radius: 45rpx; box-shadow: 0 10rpx 30rpx rgba($primary, 0.3);
    display: flex; align-items: center; justify-content: center;
    color: $white; font-size: 30rpx; font-weight: bold;
    .fab-icon { width: 40rpx; height: 40rpx; margin-right: 12rpx; }
    &:active { transform: scale(0.96); opacity: 0.9; }
  }
}

.safe-area-bottom { height: 160rpx; }
</style>