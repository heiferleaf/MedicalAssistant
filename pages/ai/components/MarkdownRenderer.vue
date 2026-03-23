<template>
	<view class="markdown-content">
		<!-- 使用 rich-text 渲染 HTML -->
		<rich-text :nodes="htmlContent" class="markdown-html"></rich-text>
	</view>
</template>

<script>
import Markdown from 'markdown-it';

// 初始化 markdown-it
const md = new Markdown({
	html: false, // 禁用 HTML 标签，防止 XSS
	linkify: true, // 自动转换 URL 为链接
	typographer: true, // 启用排版增强
	breaks: true, // 转换换行符为 <br>
});

export default {
	name: 'MarkdownRenderer',
	props: {
		content: {
			type: String,
			default: ''
		}
	},
	computed: {
		htmlContent() {
			if (!this.content) return '';
			
			try {
				// 使用 markdown-it 渲染 Markdown 为 HTML
				const html = md.render(this.content);
				return html;
			} catch (error) {
				console.error('Markdown 渲染失败:', error);
				return this.content; // 渲染失败时返回原文本
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.markdown-content {
	width: 100%;
	line-height: 1.8;
	font-size: 28rpx;
}

.markdown-html {
	width: 100%;
	
	/* 段落 */
	:deep(p) {
		margin: 16rpx 0;
		line-height: 1.8;
		color: #1e293b;
		
		@media (prefers-color-scheme: dark) {
			color: #f1f5f9;
		}
	}
	
	/* 标题 */
	:deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
		margin: 32rpx 0 16rpx;
		font-weight: 600;
		line-height: 1.4;
		color: #0f172a;
		
		@media (prefers-color-scheme: dark) {
			color: #f1f5f9;
		}
	}
	
	:deep(h1) {
		font-size: 40rpx;
		border-bottom: 2rpx solid #e2e8f0;
		padding-bottom: 8rpx;
		
		@media (prefers-color-scheme: dark) {
			border-bottom-color: #334155;
		}
	}
	
	:deep(h2) {
		font-size: 36rpx;
		border-bottom: 1rpx solid #e2e8f0;
		padding-bottom: 6rpx;
		
		@media (prefers-color-scheme: dark) {
			border-bottom-color: #334155;
		}
	}
	
	:deep(h3) {
		font-size: 32rpx;
	}
	
	:deep(h4), :deep(h5), :deep(h6) {
		font-size: 28rpx;
	}
	
	/* 粗体 */
	:deep(strong) {
		font-weight: 700;
		color: #0f172a;
		
		@media (prefers-color-scheme: dark) {
			color: #f1f5f9;
		}
	}
	
	/* 斜体 */
	:deep(em) {
		font-style: italic;
	}
	
	/* 代码块 */
	:deep(pre) {
		margin: 24rpx 0;
		padding: 24rpx;
		background: #f1f5f9;
		border-radius: 12rpx;
		overflow-x: auto;
		font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
		font-size: 24rpx;
		line-height: 1.6;
		
		@media (prefers-color-scheme: dark) {
			background: #1e293b;
		}
	}
	
	:deep(code) {
		font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
		font-size: 24rpx;
		padding: 4rpx 8rpx;
		margin: 0 4rpx;
		background: rgba(175, 184, 193, 0.2);
		border-radius: 8rpx;
		color: #475569;
		
		@media (prefers-color-scheme: dark) {
			background: rgba(71, 85, 105, 0.3);
			color: #94a3b8;
		}
	}
	
	:deep(pre code) {
		padding: 0;
		margin: 0;
		background: transparent;
		color: inherit;
	}
	
	/* 列表 */
	:deep(ul), :deep(ol) {
		margin: 16rpx 0;
		padding-left: 40rpx;
	}
	
	:deep(li) {
		margin: 8rpx 0;
		line-height: 1.6;
	}
	
	/* 引用 */
	:deep(blockquote) {
		margin: 24rpx 0;
		padding: 16rpx 24rpx;
		border-left: 6rpx solid #3B82F6;
		background: rgba(59, 130, 246, 0.1);
		border-radius: 8rpx;
		color: #64748b;
		font-style: italic;
		
		@media (prefers-color-scheme: dark) {
			background: rgba(59, 130, 246, 0.15);
			color: #94a3b8;
		}
	}
	
	/* 链接 */
	:deep(a) {
		color: #3B82F6;
		text-decoration: none;
		border-bottom: 1rpx solid transparent;
		transition: border-color 0.2s;
		
		&:hover {
			border-bottom-color: #3B82F6;
		}
		
		@media (prefers-color-scheme: dark) {
			color: #60a5fa;
		}
	}
	
	/* 表格 */
	:deep(table) {
		width: 100%;
		margin: 24rpx 0;
		border-collapse: collapse;
	}
	
	:deep(th), :deep(td) {
		padding: 16rpx;
		border: 1rpx solid #e2e8f0;
		text-align: left;
		
		@media (prefers-color-scheme: dark) {
			border-color: #334155;
		}
	}
	
	:deep(th) {
		background: #f1f5f9;
		font-weight: 600;
		
		@media (prefers-color-scheme: dark) {
			background: #1e293b;
		}
	}
	
	/* 分隔线 */
	:deep(hr) {
		margin: 32rpx 0;
		border: none;
		border-top: 1rpx solid #e2e8f0;
		
		@media (prefers-color-scheme: dark) {
			border-top-color: #334155;
		}
	}
	
	/* 图片 */
	:deep(img) {
		max-width: 100%;
		height: auto;
		margin: 16rpx 0;
		border-radius: 12rpx;
	}
}
</style>
