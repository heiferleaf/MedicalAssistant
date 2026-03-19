<script>
import { connect, closeConnection } from './config/config';

export default {
    onLaunch: function () {
        // 只有在确定有 token 的情况下才连接，否则在登录成功后再手动调用
        const token = uni.getStorageSync('accessToken');
        if (token) {
            uni.navigateTo({
                url: '/pages/index/index'
            });
            connect();
        }
        // #ifdef APP-PLUS
        plus.push.addEventListener('click', function (msg) {
            // msg.payload 就是你在 createMessage 时传入的第二个参数
            const data = typeof msg.payload === 'string' ? JSON.parse(msg.payload) : msg.payload;

            // 根据类型跳转页面
            if (data.type === "medicine_alarm") {
                uni.navigateTo({
                    url: '/pages/health/detail?id=' + data.id
                });
            }
        }, false);
        // #endif

    },
    onShow: function () {
        // 可以在这里检查连接状态，如果断开了就重连
    },
    onHide: function () {
        // 通常不需要在 Hide 时关闭，除非业务要求
    },
    onUnload: function () {
        closeConnection(); // 销毁时释放资源 [cite: 334]
    }
}
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
  font-family: 'MaterialIcons';
  src: url('~@/static/fonts/iconfont.ttf') format('truetype');
}

/* 这里的类名一定要对应到页面中使用的类名 */
.material-symbols-outlined {
  font-family: 'MaterialIcons' !important; /* 必须与上面定义的名称一致 */
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
