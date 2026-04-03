-- ====================================
-- 1. 健康数据表
-- ====================================
CREATE TABLE IF NOT EXISTS health_data (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id             BIGINT                       NOT NULL COMMENT '用户ID',
    heart_rate          DOUBLE                       NULL COMMENT '平均心率',
    step_count          INT                          NULL COMMENT '步数',
    sleep_duration      DOUBLE                       NULL COMMENT '睡眠时长（小时）',
    sleep_scope         INT                          NULL COMMENT '睡眠质量评分（0-100）',
    bloodOxygen         DOUBLE                       NULL COMMENT '平均血氧值',
    relax_type          VARCHAR(20)                  NULL COMMENT '放松类型（如冥想、呼吸、游戏）',
    relax_sub_type      VARCHAR(50)                  NULL COMMENT '放松子类型（如冥想-正念、呼吸-4-7-8、游戏-数独）',
    relax_duration      DOUBLE                       NULL COMMENT '放松时长（秒）',
    pressure_max_score  INT                          NULL COMMENT '压力最大值评分（0-100）',
    pressure_min_score  INT                          NULL COMMENT '压力最小值评分（0-100）',
    pressure_avg_score  INT                          NULL COMMENT '压力平均值评分（0-100）',
    measure_time        DATETIME                    NOT NULL COMMENT '测量时间',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    is_deleted          TINYINT DEFAULT 0           NULL COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
