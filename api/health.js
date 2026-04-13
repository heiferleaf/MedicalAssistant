import {httpRequest} from "../utils/api"

// 同步API
const sync = (params) => {
  return httpRequest('/health/sync', 'POST', params)
}

export default {
  sync
}