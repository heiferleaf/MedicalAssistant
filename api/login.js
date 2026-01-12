import {httpRequest} from "../utils/api"

// 登录API
const login = (loginData) => {
  return httpRequest('/user/login', 'POST', loginData)
}

// 注册API
const register = (registerData) => {
  return httpRequest('/user/register', 'POST', registerData)
}

export default {
  login,
  register
}