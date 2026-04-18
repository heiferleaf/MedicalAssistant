-- Tool Execution Pending Table
-- 用于存储待用户确认的 Tool 执行请求

CREATE TABLE IF NOT EXISTS `tool_execution_pending` (
    `request_id` VARCHAR(64) NOT NULL COMMENT 'Tool 执行请求 ID（唯一）',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `tool_name` VARCHAR(128) NOT NULL COMMENT 'Tool 名称（如：createPlan, updatePlan, deletePlan）',
    `tool_arguments` TEXT NOT NULL COMMENT 'Tool 参数（JSON 格式）',
    `original_ai_message` TEXT COMMENT 'AI 原始回复内容',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, APPROVED, REJECTED, EXECUTED, EXPIRED',
    `edited_data` TEXT COMMENT '用户编辑后的数据（JSON 格式，用户修改后保存）',
    `executed_at` DATETIME COMMENT '执行时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间（默认 30 分钟）',
    
    PRIMARY KEY (`request_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tool 执行待确认表';

-- 清理过期数据的定时任务（可选）
-- 删除 30 分钟前过期的记录
-- DELETE FROM tool_execution_pending WHERE expires_at < NOW();
