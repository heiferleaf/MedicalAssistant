<script>
import { connect, closeConnection } from "./config/config";
import oppoHealthManager from "./utils/oppoHealthManager.js";
export default {
  onLaunch: function () {
    // 只有在确定有 token 的情况下才连接，否则在登录成功后再手动调用
    const token = uni.getStorageSync("accessToken");
    if (token) {
      uni.navigateTo({
        url: "/pages/index/index",
      });
      connect();
    }
    // #ifdef APP-PLUS
    plus.push.addEventListener(
      "click",
      function (msg) {
        // msg.payload 就是你在 createMessage 时传入的第二个参数
        const data =
          typeof msg.payload === "string"
            ? JSON.parse(msg.payload)
            : msg.payload;

        // 根据类型跳转页面
        if (data.type === "medicine_alarm") {
          uni.navigateTo({
            url: "/pages/health/detail?id=" + data.id,
          });
        }
      },
      false
    );
    // #endif
  },
  onShow: function () {
    // 可以在这里检查连接状态，如果断开了就重连
    // oppoHealthManager.consoleLog(
    //   "App onShow: Checking connection and refreshing data..."
    // );
    this.getHealthData(); // 进入前台时获取最新数据
  },
  onHide: function () {
    // 通常不需要在 Hide 时关闭，除非业务要求
    // oppoHealthManager.consoleLog("App onHide: Pausing data updates...");
  },
  onUnload: function () {
    closeConnection(); // 销毁时释放资源 [cite: 334]
  },
  methods: {
    // 其他全局方法可以放在这里
    async getHealthData() {
      // 进入前台时获取最新数据
      const hasAuthorized = uni.getStorageSync("HEALTH_AUTHORIZED");
      if (!hasAuthorized) this.showAuthDialog();
      const token = uni.getStorageSync("accessToken");
      if (token) {
        await oppoHealthManager.fetchAllAndCache();
      }
    },
    // 显示授权弹窗或跳转授权页
    async showAuthDialog() {
      uni.showModal({
        title: '授权提示',
        content: '为了记录您的健康数据，需要授权访问OPPO健康数据',
        confirmText: '去授权',
        success: async(res) => {
          if (res.confirm) {
            // 延迟一下确保弹窗关闭
            setTimeout(async () => {
              try {
                await oppoHealthManager.init();
                await oppoHealthManager.auth();
              } catch (error) {
                console.error('授权失败', error);
              }
            }, 100);
          }
        }
      });
      uni.hideModal();
    },
  },
};
</script>

<style>
/*每个页面公共css */
.icon {
  width: 36rpx;
  height: 36rpx;

  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

@font-face {
  font-family: "MaterialIcons";
  src: url("~@/static/fonts/iconfont.ttf") format("truetype");
}

/* 这里的类名一定要对应到页面中使用的类名 */
.material-symbols-outlined {
  font-family: "MaterialIcons" !important; /* 必须与上面定义的名称一致 */
  font-weight: normal;
  font-style: normal;
  font-size: 22px; /* 默认大小，页面里可以单独调 */
  line-height: 1;
  display: inline-block;
  white-space: nowrap;
  word-wrap: normal;
  direction: ltr;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
  -moz-osx-font-smoothing: grayscale;
}
</style>
