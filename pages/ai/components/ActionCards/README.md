# AI 聊天界面操作卡片组件

## 概述

本目录包含 AI 聊天界面的智能操作卡片组件，用于实现 AI 对话与其他功能模块的联动。

## 组件结构

### 1. PlanActionCard.vue - 用药计划操作卡片

**位置**: `pages/ai/components/ActionCards/PlanActionCard.vue`

**功能**:
- 显示用药计划详情
- 提供确认创建、编辑、查看、删除等操作按钮
- 支持状态显示（待确认、进行中、已完成）

**属性**:
```javascript
{
  actionType: String,      // 操作类型：create, view, edit, delete
  actionData: Object,      // 计划数据 {medicineName, timePoint, dosage, frequency, startDate}
  showConfirm: Boolean,    // 是否显示确认按钮
  showEdit: Boolean,       // 是否显示编辑按钮
  showView: Boolean,       // 是否显示查看详情按钮
  showDelete: Boolean,     // 是否显示删除按钮
  status: String           // 状态：pending, active, completed
}
```

**事件**:
```javascript
@confirm   // 确认创建
@edit      // 编辑
@view      // 查看详情
@delete    // 删除
```

## 使用方法

### 1. 在对话中显示操作卡片

```javascript
import { createMessageWithAction } from '@/pages/ai/utils/chatUtils';

// 创建带操作卡片的消息
const planData = {
  actionType: 'create',
  data: {
    medicineName: '阿司匹林肠溶片',
    timePoint: '08:00',
    dosage: '100mg',
    frequency: '每日一次',
    startDate: '2026-03-17'
  },
  showConfirm: true,
  showEdit: true,
  showView: false,
  showDelete: false,
  status: 'pending'
};

const msg = createMessageWithAction(
  'assistant',
  '我为您准备了一个用药计划，请确认是否创建：',
  'plan',  // 操作类型
  planData  // 操作数据
);

this.messages.push(msg);
```

### 2. 处理操作卡片事件

在 Assistant.vue 中监听事件：

```javascript
<ChatView
  @action-confirm="handleActionConfirm"
  @action-edit="handleActionEdit"
  @action-view="handleActionView"
  @action-delete="handleActionDelete"
/>

methods: {
  handleActionConfirm({ type, data }) {
    if (type === 'plan') {
      this.handleCreatePlan(data);
    }
  },
  
  handleCreatePlan(data) {
    // 创建用药计划的逻辑
    console.log('创建计划:', data);
  }
}
```

## 设计风格

卡片设计遵循项目整体风格：

- **圆角**: 24rpx 大圆角
- **阴影**: 柔和的投影效果
- **渐变色**: 紫色渐变主题色
- **图标**: Emoji 图标增强视觉识别
- **响应式**: 支持深色模式

## 扩展其他卡片类型

要添加其他类型的操作卡片（如任务、家庭成员等）：

1. 在 `pages/ai/components/ActionCards/` 目录下创建新的卡片组件
2. 在 `ChatMessage.vue` 中添加对新卡片类型的支持
3. 在 `chatUtils.js` 中添加相应的数据创建函数
4. 在 `Assistant.vue` 中添加对应的事件处理逻辑

示例：

```javascript
// 创建任务卡片
const taskMsg = createMessageWithAction(
  'assistant',
  '为您创建了一个健康任务：',
  'task',  // 新的操作类型
  taskData
);
```

## API 接口

后端可以通过返回特定格式的数据来触发操作卡片：

```json
{
  "assistant_message": "我为您准备了一个用药计划",
  "action_type": "plan",
  "action_data": {
    "actionType": "create",
    "data": {
      "medicineName": "阿司匹林肠溶片",
      "timePoint": "08:00",
      "dosage": "100mg"
    },
    "showConfirm": true,
    "showEdit": true,
    "status": "pending"
  }
}
```

## 测试

1. 在 AI 对话界面点击底部输入框的"+"按钮
2. 或发送包含"用药计划"、"创建计划"、"提醒"等关键词的消息
3. 查看是否显示用药计划卡片
4. 测试卡片上的各个操作按钮

## 后续开发计划

- [ ] 实现与后端 API 的真正对接
- [ ] 添加更多类型的操作卡片（任务、家庭成员、健康数据等）
- [ ] 优化卡片动画效果
- [ ] 添加卡片模板库
- [ ] 支持卡片自定义配置
