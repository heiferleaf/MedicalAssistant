<template>
  <!-- 空组件，仅作为 renderjs 的载体 -->
  <view
    :renderjsData="renderjsData"
    :change:renderjsData="chat.startChatCore"
    :stopCount="stopCount"
    :change:stopCount="chat.stopChatCore"
  />
</template>

<script>
/**
 * App 端 SSE 流式通信组件
 * 使用 renderjs 运行在 WebView 层，支持完整的 Web API
 */
export default {
  name: 'ChatSSEClient',
  props: {
    // 配置参数（可选，主要通过方法调用）
    url: {
      type: String,
      default: ''
    },
    headers: {
      type: Object,
      default: () => ({})
    },
    body: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      // 触发 renderjs 执行的数据
      renderjsData: {
        key: 0,
        url: '',
        headers: {},
        body: {}
      },
      // 停止计数器
      stopCount: 0,
      // 当前配置
      currentConfig: null
    }
  },
  methods: {
    /**
     * 开始 SSE 流式请求
     * @param {Object} config - 配置对象
     * @param {String} config.url - 请求 URL
     * @param {Object} config.headers - 请求头
     * @param {Object} config.body - 请求体
     * @param {Function} config.onMessage - 消息回调
     * @param {Function} config.onDone - 完成回调
     * @param {Function} config.onError - 错误回调
     */
    startChat(config) {
      console.log('ChatSSEClient: 开始流式请求', config.url)
      
      this.currentConfig = config
      
      // 触发 renderjs 执行
      this.renderjsData = {
        key: Date.now(),
        url: config.url,
        headers: config.headers || {},
        body: config.body || {}
      }
    },
    
    /**
     * 停止流式请求
     */
    stopChat() {
      console.log('ChatSSEClient: 停止流式请求')
      this.stopCount++
      
      if (this.currentConfig && this.currentConfig.onDone) {
        this.currentConfig.onDone()
      }
    },
    
    /**
     * renderjs 调用：连接打开
     */
    onOpen() {
      console.log('ChatSSEClient: SSE 连接已打开')
      this.$emit('onOpen')
    },
    
    /**
     * renderjs 调用：收到消息
     * @param {String} msg - 消息内容
     */
    onMessage(msg) {
      // console.log('ChatSSEClient: 收到消息', msg)
      this.$emit('onMessage', msg)
      
      if (this.currentConfig && this.currentConfig.onMessage) {
        this.currentConfig.onMessage(msg)
      }
    },
    
    /**
     * renderjs 调用：发生错误
     * @param {Error} err - 错误对象
     */
    onError(err) {
      console.error('ChatSSEClient: SSE 错误', err)
      this.$emit('onError', err)
      
      if (this.currentConfig && this.currentConfig.onError) {
        this.currentConfig.onError(err)
      }
    },
    
    /**
     * renderjs 调用：请求完成
     */
    onFinish() {
      console.log('ChatSSEClient: SSE 请求完成')
      this.$emit('onFinish')
      
      if (this.currentConfig && this.currentConfig.onDone) {
        this.currentConfig.onDone()
      }
    }
  }
}
</script>

<script module="chat" lang="renderjs">
/**
 * renderjs 层：运行在 WebView，支持完整的 Web API
 * 使用 @microsoft/fetch-event-source 处理 SSE
 */
import { fetchEventSource } from '@microsoft/fetch-event-source'

export default {
  data() {
    return {
      // 当前控制器
      ctrl: null
    }
  },
  methods: {
    /**
     * 开始 SSE 流式请求（renderjs 环境）
     */
    startChatCore(newVal, oldVal, ownerInstance, instance) {
      if (!newVal || !newVal.url) {
        return
      }
      
      console.log('renderjs: 开始 SSE 请求', newVal.url)
      
      // 如果已有连接，先停止
      if (this.ctrl) {
        this.ctrl.abort()
        this.ctrl = null
      }
      
      try {
        // 创建 AbortController
        this.ctrl = new AbortController()
        
        console.log('renderjs: 发送请求，URL:', newVal.url)
        
        // 使用 fetch-event-source 发起 SSE 请求
        // 注意：所有参数已经在 URL 中，不需要 body
        fetchEventSource(newVal.url, {
          method: 'POST',  // 后端要求 POST
          signal: this.ctrl.signal,
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            ...newVal.headers
          },
          // 不发送 body，因为参数都在 URL 中
          
          // 连接打开
          onopen: () => {
            console.log('renderjs: SSE 连接已打开')
            ownerInstance.callMethod('onOpen')
          },
          
          // 收到消息
          onmessage: (msg) => {
            console.log('renderjs: 收到消息', msg.data, '长度:', msg.data.length)
            
            // 过滤 [DONE] 标记
            if (msg.data === '[DONE]') {
              console.log('renderjs: 过滤 [DONE]')
              return
            }
            
            ownerInstance.callMethod('onMessage', msg.data)
          },
          
          // 发生错误
          onerror: (err) => {
            console.error('renderjs: SSE 错误', err)
            ownerInstance.callMethod('onError', err)
            
            // 抛出错误，让 fetchEventSource 停止重试
            throw err
          }
        }).then(() => {
          console.log('renderjs: SSE 请求完成')
          ownerInstance.callMethod('onFinish')
        }).catch(err => {
          console.error('renderjs: SSE 请求失败', err)
          // 如果不是手动中止，则通知错误
          if (err.name !== 'AbortError') {
            ownerInstance.callMethod('onError', err)
          }
        })
        
      } catch (e) {
        console.error('renderjs: SSE 请求异常', e)
        ownerInstance.callMethod('onError', e)
      }
    },
    
    /**
     * 停止 SSE 请求（renderjs 环境）
     */
    stopChatCore(newVal, oldVal, ownerInstance, instance) {
      console.log('renderjs: 停止 SSE 请求')
      
      if (this.ctrl) {
        this.ctrl.abort()
        this.ctrl = null
      }
    }
  }
}
</script>

<style scoped>
/* 隐藏组件，仅作为功能载体 */
view {
  display: none;
}
</style>
