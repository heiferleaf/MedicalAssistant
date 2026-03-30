<template>
	<view class="simple-markdown">
		<!-- #ifdef H5 -->
		<!-- H5 使用 v-html 渲染 -->
		<div v-html="renderedHTML"></div>
		<!-- #endif -->
		
		<!-- #ifdef APP-PLUS -->
		<!-- App 端使用 rich-text 渲染 -->
		<rich-text :nodes="renderedHTML"></rich-text>
		<!-- #endif -->
	</view>
</template>

<script>
// #ifdef H5
import { marked } from 'marked';
// #endif

export default {
	name: 'SimpleMarkdown',
	props: {
		content: {
			type: String,
			default: ''
		}
	},
	data() {
		return {
			renderedHTML: ''
		}
	},
	watch: {
		content: {
			immediate: true,
			handler(newVal) {
				if (newVal) {
					this.renderMarkdown(newVal)
				}
			}
		}
	},
	mounted() {
		if (this.content) {
			this.renderMarkdown(this.content)
		}
	},
	methods: {
		renderMarkdown(text) {
			// #ifdef H5
			// H5 端使用 marked 渲染
			try {
				this.renderedHTML = marked.parse(text, {
					breaks: true, // 支持换行
					gfm: true // GitHub Flavored Markdown
				})
			} catch (e) {
				console.error('Markdown 渲染失败:', e)
				this.renderedHTML = text
			}
			// #endif
			
			// #ifdef APP-PLUS
			// App 端通过 renderjs 渲染
			this.renderedHTML = this.renderMarkdownForApp(text)
			// #endif
		},
		
		// #ifdef APP-PLUS
		renderMarkdownForApp(text) {
			// 简单的 Markdown 转 HTML 实现（用于 App 端）
			let html = text
			
			// 标题
			html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>')
			html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>')
			html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>')
			
			// 粗体
			html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
			
			// 斜体
			html = html.replace(/\*(.*?)\*/g, '<em>$1</em>')
			
			// 代码块
			html = html.replace(/`(.*?)`/g, '<code>$1</code>')
			
			// 列表
			html = html.replace(/^- (.*$)/gim, '<li>$1</li>')
			html = html.replace(/^(\d+)\. (.*$)/gim, '<li>$2</li>')
			
			// 换行
			html = html.replace(/\n/g, '<br>')
			
			return html
		}
		// #endif
	}
}
</script>

<style lang="scss" scoped>
.simple-markdown {
	width: 100%;
	line-height: 1.6;
	font-size: 28rpx;
}

/* H5 样式 */
/* #ifdef H5 */
:deep(h1) {
	font-size: 36rpx;
	font-weight: bold;
	margin: 16rpx 0;
}

:deep(h2) {
	font-size: 32rpx;
	font-weight: bold;
	margin: 14rpx 0;
}

:deep(h3) {
	font-size: 30rpx;
	font-weight: bold;
	margin: 12rpx 0;
}

:deep(strong) {
	font-weight: bold;
}

:deep(em) {
	font-style: italic;
}

:deep(code) {
	font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
	padding: 4rpx 8rpx;
	margin: 0 4rpx;
	background: rgba(175, 184, 193, 0.2);
	border-radius: 8rpx;
	font-size: 24rpx;
}

:deep(li) {
	margin-left: 20rpx;
	margin-bottom: 8rpx;
}

:deep(p) {
	margin-bottom: 12rpx;
}
/* #endif */

/* App 端样式 */
/* #ifdef APP-PLUS */
.simple-markdown >>> h1 {
	font-size: 36rpx;
	font-weight: bold;
	margin: 16rpx 0;
}

.simple-markdown >>> h2 {
	font-size: 32rpx;
	font-weight: bold;
	margin: 14rpx 0;
}

.simple-markdown >>> h3 {
	font-size: 30rpx;
	font-weight: bold;
	margin: 12rpx 0;
}

.simple-markdown >>> strong {
	font-weight: bold;
}

.simple-markdown >>> em {
	font-style: italic;
}

.simple-markdown >>> code {
	font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
	padding: 4rpx 8rpx;
	margin: 0 4rpx;
	background: rgba(175, 184, 193, 0.2);
	border-radius: 8rpx;
	font-size: 24rpx;
}

.simple-markdown >>> li {
	margin-left: 20rpx;
	margin-bottom: 8rpx;
}
/* #endif */
</style>
