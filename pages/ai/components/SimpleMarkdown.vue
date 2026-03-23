<template>
	<view class="simple-markdown">
		<view 
			v-for="(segment, index) in parsedSegments" 
			:key="index"
			class="markdown-segment"
		>
			<text 
				v-for="(part, partIndex) in segment.parts" 
				:key="partIndex"
				:class="getPartClass(part)"
			>{{ part.content }}</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'SimpleMarkdown',
	props: {
		content: {
			type: String,
			default: ''
		}
	},
	computed: {
		parsedSegments() {
			if (!this.content) return [];
			
			// 按换行分割成段落
			const lines = this.content.split('\n');
			const segments = [];
			
			for (let line of lines) {
				if (!line.trim()) {
					// 空行跳过
					continue;
				}
				
				// 解析每一行的 Markdown 格式
				const parts = this.parseLine(line);
				segments.push({
					parts: parts
				});
			}
			
			return segments;
		}
	},
	methods: {
		parseLine(line) {
			const parts = [];
			let currentText = '';
			let i = 0;
			
			while (i < line.length) {
				// 检查粗体 **text**
				if (i + 1 < line.length && line[i] === '*' && line[i + 1] === '*') {
					// 添加之前的普通文本
					if (currentText) {
						parts.push({ type: 'normal', content: currentText });
						currentText = '';
					}
					
					// 找到结束的 **
					i += 2;
					let boldText = '';
					while (i < line.length) {
						if (i + 1 < line.length && line[i] === '*' && line[i + 1] === '*') {
							i += 2;
							break;
						}
						boldText += line[i];
						i++;
					}
					
					if (boldText) {
						parts.push({ type: 'bold', content: boldText });
					}
				}
				// 检查斜体 *text*
				else if (line[i] === '*') {
					// 添加之前的普通文本
					if (currentText) {
						parts.push({ type: 'normal', content: currentText });
						currentText = '';
					}
					
					// 找到结束的 *
					i += 1;
					let italicText = '';
					while (i < line.length) {
						if (line[i] === '*') {
							i += 1;
							break;
						}
						italicText += line[i];
						i++;
					}
					
					if (italicText) {
						parts.push({ type: 'italic', content: italicText });
					}
				}
				// 检查代码 `text`
				else if (line[i] === '`') {
					// 添加之前的普通文本
					if (currentText) {
						parts.push({ type: 'normal', content: currentText });
						currentText = '';
					}
					
					// 找到结束的 `
					i += 1;
					let codeText = '';
					while (i < line.length) {
						if (line[i] === '`') {
							i += 1;
							break;
						}
						codeText += line[i];
						i++;
					}
					
					if (codeText) {
						parts.push({ type: 'code', content: codeText });
					}
				}
				// 普通文本
				else {
					currentText += line[i];
					i++;
				}
			}
			
			// 添加最后的普通文本
			if (currentText) {
				parts.push({ type: 'normal', content: currentText });
			}
			
			return parts;
		},
		getPartClass(part) {
			const classes = ['markdown-part'];
			if (part.type === 'bold') {
				classes.push('markdown-bold');
			} else if (part.type === 'italic') {
				classes.push('markdown-italic');
			} else if (part.type === 'code') {
				classes.push('markdown-code');
			}
			return classes;
		}
	}
}
</script>

<style lang="scss" scoped>
.simple-markdown {
	width: 100%;
	line-height: 1.8;
}

.markdown-segment {
	margin-bottom: 8rpx;
	display: flex;
	flex-wrap: wrap;
	align-items: flex-start;
}

.markdown-part {
	display: inline;
	word-wrap: break-word;
	word-break: break-all;
}

.markdown-bold {
	font-weight: 700;
}

.markdown-italic {
	font-style: italic;
}

.markdown-code {
	font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
	padding: 4rpx 8rpx;
	margin: 0 4rpx;
	background: rgba(175, 184, 193, 0.2);
	border-radius: 8rpx;
	font-size: 24rpx;
}
</style>
