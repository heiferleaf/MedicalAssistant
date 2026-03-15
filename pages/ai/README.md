# AI 对话界面架构说明

## 📁 目录结构

```
pages/ai/
├── Assistant.vue          # 主入口（协调器）
├── components/            # 可复用组件
│   ├── ChatHeader.vue    # 聊天头部
│   ├── ChatMessage.vue   # 消息气泡
│   ├── ChatInput.vue     # 输入框
│   ├── ShortcutBar.vue   # 快捷短语栏
│   └── SessionSidebar.vue # 会话侧边栏
├── views/                # 视图组件
│   ├── ChatView.vue     # 聊天主视图
│   └── WelcomeView.vue  # 欢迎视图
└── utils/               # 工具函数
    └── chatUtils.js    # 聊天相关工具
```

## 🏗️ 架构设计原则

### 1. **单一职责原则**
每个组件只负责一个功能：
- `ChatHeader` - 只显示头部信息
- `ChatMessage` - 只渲染单条消息
- `ChatInput` - 只处理输入和发送
- `SessionSidebar` - 只显示会话列表

### 2. **高内聚低耦合**
- 组件之间通过 props 和 events 通信
- 主入口 `Assistant.vue` 负责状态管理和业务逻辑
- 工具函数独立，易于测试和复用

### 3. **可扩展性**
- 新增消息类型：修改 `ChatMessage.vue` 或使用插槽
- 新增视图：在 `views/` 下创建新组件
- 新增功能：在 `components/` 下创建新组件

## 🔧 组件说明

### Assistant.vue (主入口)
**职责**：
- 状态管理（messages, sessions, currentSessionId 等）
- 业务逻辑（发送消息、切换会话等）
- 组件协调

**不做什么**：
- 不直接渲染 UI（交给子组件）
- 不处理具体交互细节

### ChatHeader.vue
**Props**:
- `showMenu`: 是否显示菜单按钮
- `avatarText`: 头像文字
- `title`: 标题
- `statusText`: 状态文字

**Events**:
- `toggle-sidebar`: 切换侧边栏

### ChatMessage.vue
**Props**:
- `role`: 'user' | 'assistant' | 'loading'
- `type`: 'text' | 'image' | ...
- `content`: 消息内容
- `image`: 图片 URL

**特点**:
- 支持插槽扩展特殊消息类型
- 自动根据 role 应用样式

### ChatInput.vue
**Props**:
- `placeholder`: 占位符
- `showAddBtn`: 显示添加按钮
- `showMic`: 显示语音按钮

**Events**:
- `send`: 发送消息
- `add`: 点击添加按钮
- `voice`: 点击语音按钮

### SessionSidebar.vue
**Props**:
- `visible`: 是否显示
- `sessions`: 会话列表
- `currentSessionId`: 当前会话 ID

**Events**:
- `new`: 创建新会话
- `close`: 关闭侧边栏
- `switch`: 切换会话
- `delete`: 删除会话

## 🛠️ 工具函数 (chatUtils.js)

### 消息创建
```javascript
import { createMessage, MessageType, MessageRole } from './utils/chatUtils';

const msg = createMessage(
  MessageRole.USER,
  '你好',
  MessageType.TEXT
);
```

### 本地存储
```javascript
import { getFromStorage, setToStorage, StorageKeys } from './utils/chatUtils';

const userId = getFromStorage(StorageKeys.USER_ID);
setToStorage(StorageKeys.SESSION_ID, sessionId);
```

## 📝 使用示例

### 基础使用
```vue
<template>
  <Assistant />
</template>
```

### 扩展消息类型
```vue
<ChatMessage role="assistant" type="chart">
  <template #message-0>
    <CustomChart :data="chartData" />
  </template>
</ChatMessage>
```

### 自定义快捷短语
```vue
<ShortcutBar 
  :shortcuts="[
    { icon: '...', text: '自定义', type: 'custom' }
  ]"
  @click="handleCustom"
/>
```

## 🎨 样式定制

所有组件都支持暗黑模式：
```scss
@media (prefers-color-scheme: dark) {
  // 暗黑模式样式
}
```

## 🚀 未来扩展

### 可能的跳转动画
1. **创建新视图文件**
   ```vue
   <!-- views/AnimationView.vue -->
   <template>
     <view class="animation-view">
       <!-- 动画内容 -->
     </view>
   </template>
   ```

2. **在 Assistant.vue 中切换视图**
   ```vue
   <component :is="currentView" />
   ```

3. **添加过渡效果**
   ```vue
   <transition name="slide">
     <component :is="currentView" />
   </transition>
   ```

### 与其他界面联动
1. **接收参数**
   ```javascript
   onLoad(options) {
     const { from, data } = options;
   }
   ```

2. **跳转回其他页面**
   ```javascript
   uni.navigateTo({
     url: '/pages/index/index'
   });
   ```

## ✅ 优势

1. **易维护**：每个文件职责单一，易于理解和修改
2. **易测试**：组件独立，可单独测试
3. **易扩展**：新增功能不影响现有代码
4. **可复用**：组件可在其他页面复用
5. **类型安全**：props 和 events 明确定义

## 📞 开发建议

1. **新增功能**：先考虑是否需要新组件
2. **修改样式**：尽量在组件内部完成，不影响全局
3. **业务逻辑**：放在 `Assistant.vue` 中，保持组件纯净
4. **工具函数**：通用逻辑抽离到 `utils/`
