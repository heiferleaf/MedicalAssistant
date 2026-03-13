// API基础配置
const BASE_URL = "http://localhost:8080/api"; // 请替换为您的实际API地址

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

  // 可以在这里添加loading效果
  uni.showLoading({
    title: "加载中...",
    mask: true,
  });

  return config;
};

// 响应拦截器
const responseInterceptor = async (response, resolve, reject) => {
  uni.hideLoading();

  const { statusCode, data } = response;
  if (statusCode === 200) {
    if (data.code === 0 || data.code === 200) {
      resolve(data.data || data);
    } else {
      uni.showToast({
        title: data.message || "请求失败",
        icon: "none",
      });
      reject(data);
    }
  } else if (statusCode === 402) {
    const isOk = await handleRefreshToken();
    if (isOk) {
      // 换票成功，携带新票重新发起本次请求
      resolve(retryRes);
    } else {
      reject(response);
    }
  } else if (statusCode === 403) {
    handleLogout();
    reject(response);
  } else {
    uni.showToast({
      title: `网络错误: ${statusCode}`,
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
      success: (response) => responseInterceptor(response, resolve, reject),
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

  if (!userId || !refreshToken) return false;
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + "/user/refresh",
      method: "POST",
      data: { userId, refreshToken },
      success: (res) => {
        if (res.data.code === 200) {
          // 刷新成功：data 字段为字符串类型的 newAccessToken
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
      uni.reLaunch({ url: "/pages/login/login" });
    },
  });
}

export default {
  httpRequest,
};
