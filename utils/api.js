// API基础配置
const BASE_URL = 'http://localhost:8080/api' // 请替换为您的实际API地址

// 请求拦截器
const requestInterceptor = (config) => {
  // 可以从本地存储获取token
  const token = uni.getStorageSync('token')
  if (token) {
    config.header = {
      ...config.header,
      'Authorization': `Bearer ${token}`
    }
  }
  
  // 可以在这里添加loading效果
  uni.showLoading({
    title: '加载中...',
    mask: true
  })
  
  return config
}

// 响应拦截器
const responseInterceptor = (response, resolve, reject) => {
  uni.hideLoading()
  
  const { statusCode, data } = response
  if (statusCode === 200) {
    if (data.code === 0 || data.code === 200) {
      resolve(data.data || data)
    } else {
      uni.showToast({
        title: data.message || '请求失败',
        icon: 'none'
      })
      reject(data)
    }
  } else {
    uni.showToast({
      title: `网络错误: ${statusCode}`,
      icon: 'none'
    })
    reject(response)
  }
}

// 错误处理
const errorHandler = (error, reject) => {
  uni.hideLoading()
  uni.showToast({
    title: '网络请求失败，请检查网络',
    icon: 'none'
  })
  reject(error)
}

// 通用请求方法
export const httpRequest = (url, method, data = {}, header = {}) => {
  return new Promise((resolve, reject) => {
    const config = {
      url: url.startsWith('http') ? url : `${BASE_URL}${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...header
      },
      success: (response) => responseInterceptor(response, resolve, reject),
      fail: (error) => errorHandler(error, reject)
    }
    
    // 执行请求拦截器
    const finalConfig = requestInterceptor(config)
    
    uni.request(finalConfig)
  })
}

export default {
  httpRequest
}