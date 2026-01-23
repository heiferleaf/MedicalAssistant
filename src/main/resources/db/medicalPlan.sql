-- ====================================
-- 1.  药品表
-- ====================================
CREATE TABLE medicine (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '药品ID',
                          user_id BIGINT NOT NULL COMMENT '所属用户ID',
                          name VARCHAR(100) NOT NULL COMMENT '药品名称',
                          default_dosage VARCHAR(50) DEFAULT NULL COMMENT '推荐剂量（可选）',
                          remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品表';

-- ====================================
-- 2. 用药计划表（新增deleted字段）
-- ====================================
CREATE TABLE medication_plan (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
                                 user_id BIGINT NOT NULL COMMENT '所属用户ID',
                                 medicine_id BIGINT NOT NULL COMMENT '药品ID',
                                 dosage VARCHAR(50) NOT NULL COMMENT '本次计划剂量',
                                 start_date DATE NOT NULL COMMENT '开始日期',
                                 end_date DATE DEFAULT NULL COMMENT '结束日期（可空表示长期）',
                                 time_points VARCHAR(100) NOT NULL COMMENT '每日服药时间点JSON数组，如["08:00","20:00"]',
                                 remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
                                 deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标识：0-正常，1-已删除',
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 INDEX idx_user_id (user_id),
                                 INDEX idx_deleted (deleted),
                                 FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';

-- ====================================
-- 3. 用药任务表
-- ====================================
CREATE TABLE medication_task (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
                                 user_id BIGINT NOT NULL COMMENT '所属用户ID',
                                 plan_id BIGINT NOT NULL COMMENT '归属计划ID',
                                 medicine_id BIGINT NOT NULL COMMENT '药品ID（冗余字段）',
                                 dosage VARCHAR(50) NOT NULL COMMENT '剂量（冗余字段）',
                                 task_date DATE NOT NULL COMMENT '任务日期',
                                 time_point VARCHAR(10) NOT NULL COMMENT '服药时间点，如"08:00"',
                                 status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未服用，1-已服用，2-漏服',
                                 operate_time DATETIME DEFAULT NULL COMMENT '操作时间（标记状态的时间）',
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 INDEX idx_user_task_date (user_id, task_date),
                                 INDEX idx_plan_id (plan_id),
                                 INDEX idx_status (status),
                                 FOREIGN KEY (plan_id) REFERENCES medication_plan(id) ON DELETE RESTRICT,
                                 FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药任务表';