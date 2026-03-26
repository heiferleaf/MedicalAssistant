-- ====================================
-- 修复 agent_messages 表 content 字段长度
-- 原因：Base64 图片消息可能超过 64KB
-- ====================================

-- 修改 agent_messages 表的 content 字段为 MEDIUMTEXT
ALTER TABLE agent_messages 
MODIFY COLUMN content MEDIUMTEXT NOT NULL COMMENT '消息内容（支持大文本，如 Base64 图片）';

-- 验证修改
DESCRIBE agent_messages;
