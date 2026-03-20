/**
 * 简单的 Markdown 清理工具
 */

/**
 * 清理 Markdown 格式，保留纯文本
 * @param {string} markdown - Markdown 文本
 * @returns {string} 清理后的纯文本
 */
export function cleanMarkdown(markdown) {
  if (!markdown) return '';
  
  let text = markdown;
  
  // 先处理换行
  text = text.replace(/\n/g, '\n');
  
  // 移除粗体 **text**
  text = text.replace(/\*\*(.*?)\*\*/g, '$1');
  
  // 移除斜体 *text*
  text = text.replace(/\*(.*?)\*/g, '$1');
  
  // 移除代码 `code`
  text = text.replace(/`([^`]+)`/g, '$1');
  
  // 移除标题 # 到 ######
  text = text.replace(/^(#{1,6})\s+/gm, '');
  
  // 移除无序列表 - item
  text = text.replace(/^-\s+/gm, '');
  
  // 移除有序列表 1. item
  text = text.replace(/^\d+\.\s+/gm, '');
  
  // 移除链接 [text](url)，保留文字
  text = text.replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');
  
  // 移除分隔线 ---
  text = text.replace(/^---$/gm, '');
  
  return text;
}

/**
 * 简单的 Markdown 解析，直接返回处理后的文本数组（用于分段渲染）
 * @param {string} markdown - Markdown 文本
 * @returns {Array} 文本段落数组
 */
export function parseMarkdownToSegments(markdown) {
  if (!markdown) return [];
  
  let text = markdown;
  
  // 按换行分割
  const lines = text.split('\n');
  const segments = [];
  
  for (let line of lines) {
    if (!line.trim()) continue;
    
    let isBold = false;
    let content = line;
    
    // 检测粗体
    if (content.includes('**')) {
      isBold = true;
      content = content.replace(/\*\*(.*?)\*\*/g, '$1');
    }
    
    // 检测标题
    let isHeading = false;
    let headingLevel = 0;
    const headingMatch = content.match(/^(#{1,6})\s+(.*)$/);
    if (headingMatch) {
      isHeading = true;
      headingLevel = headingMatch[1].length;
      content = headingMatch[2];
    }
    
    segments.push({
      content: content,
      isBold: isBold,
      isHeading: isHeading,
      headingLevel: headingLevel
    });
  }
  
  return segments;
}

export default {
  cleanMarkdown,
  parseMarkdownToSegments
};
