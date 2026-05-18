-- DomainEvent 审计表
CREATE TABLE IF NOT EXISTS domain_event_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE COMMENT '事件唯一ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型（如 medication.alarm）',
    aggregate_type VARCHAR(64) COMMENT '聚合根类型',
    aggregate_id BIGINT COMMENT '聚合根ID',
    user_id BIGINT COMMENT '用户ID',
    group_id BIGINT COMMENT '家庭组ID',
    payload JSON COMMENT '事件载荷',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSED/FAILED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    processed_at DATETIME COMMENT '处理完成时间',
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at),
    INDEX idx_user_id (user_id),
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DomainEvent 审计日志表';
