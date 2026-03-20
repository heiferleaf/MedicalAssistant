import { BASE_URL } from "../config/config";

// 请求拦截器
const requestInterceptor = (config) => {
  // 可以从本地存储获取token
  const token = uni.getStorageSync("accessToken");
  if (token) {
    config.header = {
      ...config.header,
      Authorization: `Bearer ${token}`,
    };
  }

  return config;
};

// 响应拦截器
const responseInterceptor = async (response, resolve, reject) => {
  uni.hideLoading();

  const { statusCode, data } = response;
  console.log("HTTP 响应:", statusCode, data);
  if (data.code === 200) {
    if (data.code === 0 || data.code === 200) {
      resolve(data.data || data);
    } else {
      uni.showToast({
        title: data.message || "请求失败",
        icon: "none",
      });
      reject(data);
    }
  } else if (data.code === 402) {
    console.log("Token 过期，尝试刷新...");
    const isOk = await handleRefreshToken();
    if (isOk) {
      console.log("Token 刷新成功，重试请求...");
      // 重新发起请求，此时请求拦截器会自动带上最新的 token
      httpRequest(
        originalConfig.url,
        originalConfig.method,
        originalConfig.data,
        originalConfig.header,
      )
        .then((retryRes) => resolve(retryRes))
        .catch((err) => reject(err));
    } else {
      console.log("Token 刷新失败，强制退出...");
      handleLogout();
      reject(response);
    }
  } else if (data.code === 403) {
    handleLogout();
    reject(response);
  } else {
    uni.showToast({
      title: `网络错误: ${data.code || statusCode}`,
      icon: "none",
    });
    reject(response);
  }
};

// 错误处理
const errorHandler = (error, reject) => {
  uni.hideLoading();
  uni.showToast({
    title: "网络请求失败，请检查网络",
    icon: "none",
  });
  reject(error);
};

// 通用请求方法
export const httpRequest = (url, method, data = {}, header = {}) => {
  return new Promise((resolve, reject) => {
    const config = {
      url: url.startsWith("http") ? url : `${BASE_URL}${url}`,
      method,
      data,
      header: {
        "Content-Type": "application/json",
        ...header,
      },
      success: (response) =>
        responseInterceptor(response, resolve, reject, {
          url,
          method,
          data,
          header,
        }),
      fail: (error) => errorHandler(error, reject),
    };

    // 执行请求拦截器
    const finalConfig = requestInterceptor(config);

    uni.request(finalConfig);
  });
};

/**
 * 
静默刷新逻辑 */
async function handleRefreshToken() {
  const userId = uni.getStorageSync("userId");
  const refreshToken = uni.getStorageSync("refreshToken");

  console.log("尝试刷新 Token，userId:", userId, "refreshToken:", refreshToken);
  if (!userId || !refreshToken) return false;
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + "/user/refresh",
      method: "POST",
      data: { userId: userId, refreshToken: refreshToken },
      success: (res) => {
        console.log("刷新 Token 响应:", res);
        if (res.data.code === 200) {
          // 刷新成功：data 字段为字符串类型的 newAccessToken
          console.log("新 Access Token:", res.data.data);
          uni.setStorageSync("accessToken", res.data.data);
          resolve(true);
        } else {
          // 刷新失败：可能是 403，触发强制登录handleLogout();
          resolve(false);
        }
      },
      fail: () => resolve(false),
    });
  });
}

/**
 * 
强制退出逻辑 */
function handleLogout() {
  uni.clearStorageSync(); // 清除 userId 和双 Token
  uni.showModal({
    title: "登录失效",
    content: "您的登录已过期，请重新登录",
    showCancel: false,

    success: () => {
      uni.reLaunch({ url: "/pages/Login" });
    },
  });
}

export default {
  httpRequest,
};
