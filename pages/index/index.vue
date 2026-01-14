<template>
  <view class="page-container">
    <!-- 页面内容 -->
    <view class="content">
      <!-- 根据当前tab显示不同内容 -->
      <view v-if="currentTab === 0">
        <HomePage></HomePage>
      </view>
      <view v-else-if="currentTab === 1">
        <HealthPage></HealthPage>
      </view>
	  <view v-else-if="currentTab === 2">
		<MedicinePage></MedicinePage>
	  </view>
      <view v-else>
        <MinePage></MinePage>
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

import HomePage from "./Home.vue"
import HealthPage from './Health.vue';
import MedicinePage from './Medicine.vue';
import MinePage from "./Mine.vue"

export default {
  components: {
    AppNavbar,
	HomePage,
	HealthPage,
	MedicinePage,
	MinePage
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
  overflow-y: hidden;
  /* padding-bottom: 120rpx; 给导航栏留出空间 */
}
</style>