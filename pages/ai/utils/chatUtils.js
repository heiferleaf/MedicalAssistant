/**
 * AI 聊天工具函数
 */

/**
 * 生成唯一的消息 ID
 */
export function generateMessageId() {
	return 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

/**
 * 格式化消息时间
 */
export function formatMessageTime(timestamp) {
	const date = new Date(timestamp);
	const now = new Date();
	const diff = now - date;
	
	// 小于 1 分钟
	if (diff < 60000) {
		return '刚刚';
	}
	// 小于 1 小时
	if (diff < 3600000) {
		return Math.floor(diff / 60000) + '分钟前';
	}
	// 小于 24 小时
	if (diff < 86400000) {
		return Math.floor(diff / 3600000) + '小时前';
	}
	// 小于 7 天
	if (diff < 604800000) {
		return Math.floor(diff / 86400000) + '天前';
	}
	
	// 超过 7 天显示具体日期
	return date.toLocaleDateString('zh-CN');
}

/**
 * 消息类型枚举
 */
export const MessageType = {
	TEXT: 'text',
	IMAGE: 'image',
	BRIEFING: 'briefing',
	RESULT: 'result',
	CHART: 'chart',
	ADVICE: 'advice',
	LOADING: 'loading'
};

/**
 * 角色枚举
 */
export const MessageRole = {
	USER: 'user',
	ASSISTANT: 'assistant',
	SYSTEM: 'system',
	LOADING: 'loading'
};

/**
 * 创建消息对象
 */
export function createMessage(role, content, type = MessageType.TEXT, extra = {}) {
	return {
		id: generateMessageId(),
		role,
		type,
		content,
		...extra,
		createdAt: new Date().toISOString()
	};
}

/**
 * 快捷短语配置
 */
export const defaultShortcuts = [
	{ icon: '/static/Health/pill-active.svg', text: '询问副作用', type: 'side_effect' },
	{ icon: '/static/Home/warning.svg', text: '报告不适', type: 'report_discomfort' },
	{ icon: '/static/Mine/report.svg', text: '就医清单', type: 'medical_list' }
];

/**
 * 本地存储键名
 */
export const StorageKeys = {
	SESSION_ID: 'agent_session_id',
	USER_ID: 'userId',
	ACCESS_TOKEN: 'accessToken'
};

/**
 * 从本地存储获取数据
 */
export function getFromStorage(key) {
	try {
		return uni.getStorageSync(key);
	} catch (e) {
		console.error('读取本地存储失败:', e);
		return null;
	}
}

/**
 * 存储数据到本地
 */
export function setToStorage(key, value) {
	try {
		uni.setStorageSync(key, value);
		return true;
	} catch (e) {
		console.error('写入本地存储失败:', e);
		return false;
	}
}

/**
 * 生成会话 ID
 */
export function generateSessionId() {
	return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

export default {
	generateMessageId,
	formatMessageTime,
	MessageType,
	MessageRole,
	createMessage,
	defaultShortcuts,
	StorageKeys,
	getFromStorage,
	setToStorage,
	generateSessionId
};
