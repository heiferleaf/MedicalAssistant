<template>
  <view class="page-container">
    <!-- 页面内容 -->
    <view class="content">
      <!-- 根据当前tab显示不同内容 -->
      <view v-if="currentTab === 0">
        AI助手页面内容
      </view>
      <view v-else-if="currentTab === 1">
        健康页面内容
      </view>
      <view v-else>
        我的页面内容
      </view>
    </view>
    
    <!-- 导航栏固定在底部 -->
    <app-navbar 
      :current="currentTab" 
      @change="handleTabChange"
    />
  </view>
</template>

<script>
import AppNavbar from '@/components/app-navbar.vue';

export default {
  components: {
    AppNavbar
  },
  data() {
    return {
      currentTab: 0
    };
  },
  methods: {
    handleTabChange(index) {
      this.currentTab = index;
    }
  }
};
</script>

<style scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 120rpx; /* 给导航栏留出空间 */
}
</style>