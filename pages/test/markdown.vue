<template>
	<view class="container">
		<view class="test-section">
			<view class="section-title">Markdown 渲染测试</view>
			
			<view class="test-card">
				<view class="card-title">测试 1：基础格式</view>
				<MarkdownRenderer :content="test1" />
			</view>
			
			<view class="test-card">
				<view class="card-title">测试 2：代码块</view>
				<MarkdownRenderer :content="test2" />
			</view>
			
			<view class="test-card">
				<view class="card-title">测试 3：列表和引用</view>
				<MarkdownRenderer :content="test3" />
			</view>
			
			<view class="test-card">
				<view class="card-title">测试 4：表格</view>
				<MarkdownRenderer :content="test4" />
			</view>
			
			<view class="test-card">
				<view class="card-title">测试 5：医疗场景示例</view>
				<MarkdownRenderer :content="test5" />
			</view>
		</view>
	</view>
</template>

<script>
import MarkdownRenderer from './components/MarkdownRenderer.vue';

export default {
	name: 'MarkdownTest',
	components: {
		MarkdownRenderer
	},
	data() {
		return {
			test1: `# 标题 1
## 标题 2
### 标题 3

这是**粗体**文本，这是*斜体*文本，这是 \`行内代码\`。

这是一个链接：[百度](https://www.baidu.com)`,
			
			test2: `这是行内代码：\`console.log('hello')\`

\`\`\`javascript
function test() {
  console.log('代码块测试');
  return true;
}
\`\`\`

\`\`\`python
def hello():
    print("Python 代码块")
\`\`\``,
			
			test3: `无序列表：
- 项目 1
- 项目 2
  - 子项目 2.1
  - 子项目 2.2
- 项目 3

有序列表：
1. 第一步
2. 第二步
3. 第三步

> 这是一段引用文本
> 可以有多行`,
			
			test4: `| 药品名称 | 规格 | 用量 | 频次 |
| --- | --- | --- | --- |
| 阿司匹林 | 100mg | 1 片 | 每日 1 次 |
| 布洛芬 | 200mg | 1 粒 | 每日 3 次 |
| 对乙酰氨基酚 | 500mg | 1 片 | 每日 2 次 |`,
			
			test5: `## 用药指导

**药品名称**：阿司匹林肠溶片

**规格**：100mg × 30 片

**用法用量**：
- 口服
- 每次 1 片（100mg）
- 每日 1 次，早餐前 30 分钟服用

**适应症**：
1. 预防心肌梗死
2. 预防脑卒中
3. 治疗心绞痛

**注意事项**：
> 本品应整片吞服，不可嚼碎。如有胃部不适，请立即停药并咨询医生。

**禁忌症**：
- 对阿司匹林过敏者禁用
- 活动性溃疡患者禁用
- 严重肝肾功能不全者禁用`
		}
	},
	methods: {
		
	}
}
</script>

<style lang="scss" scoped>
.container {
	padding: 40rpx;
	background: #f8fafc;
	min-height: 100vh;
	
	@media (prefers-color-scheme: dark) {
		background: #0f172a;
	}
}

.test-section {
	margin-bottom: 40rpx;
}

.section-title {
	font-size: 36rpx;
	font-weight: 700;
	color: #0f172a;
	margin-bottom: 32rpx;
	text-align: center;
	
	@media (prefers-color-scheme: dark) {
		color: #f1f5f9;
	}
}

.test-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 32rpx;
	margin-bottom: 32rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
	
	@media (prefers-color-scheme: dark) {
		background: #1e293b;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.3);
	}
}

.card-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #3B82F6;
	margin-bottom: 24rpx;
	padding-bottom: 16rpx;
	border-bottom: 2rpx solid #e2e8f0;
	
	@media (prefers-color-scheme: dark) {
		color: #60a5fa;
		border-bottom-color: #334155;
	}
}
</style>
