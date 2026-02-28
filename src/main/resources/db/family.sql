
-- ====================================
-- 对用户表进行修改
-- ====================================
alter table t_user add phone_number VARCHAR(11) not null unique comment "手机号";


-- ====================================
-- 4. 家庭组表
-- ====================================
CREATE TABLE family_group (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '家庭组唯一ID，自增主键',
                              group_name VARCHAR(64) NOT NULL COMMENT '家庭组名称',
                              owner_user_id BIGINT NOT NULL COMMENT '组长用户ID',
                              description VARCHAR(256) DEFAULT NULL COMMENT '家庭组简介或补充说明',
                              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
                              is_deleted BOOLEAN DEFAULT 0 COMMENT '0=正常，1=软删除（逻辑删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭组表';

-- ====================================
-- 5. 家庭组成员表
-- ====================================
CREATE TABLE family_member (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '家庭组成员唯一ID，自增主键',
                               group_id BIGINT NOT NULL COMMENT '所属家庭组ID，关联family_group.id',
                               user_id BIGINT NOT NULL COMMENT '用户ID',
                               role VARCHAR(16) NOT NULL COMMENT '成员角色，leader=组长，normal=普通成员',
                               join_time DATETIME NOT NULL COMMENT '加入家庭组时间',
                               status VARCHAR(16) NOT NULL COMMENT '成员状态：active=在组，quit=退出',
                               exit_time DATETIME DEFAULT NULL COMMENT '退出时间，未退出为NULL',
                               is_deleted BOOLEAN DEFAULT 0 COMMENT '0=正常，1=软删除（逻辑删除）',
                               INDEX idx_group_user (group_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭组成员表';

-- ====================================
-- 6. 健康数据表
-- ====================================
CREATE TABLE health_data (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '健康数据唯一ID，自增主键',
                             user_id BIGINT NOT NULL COMMENT '用户ID，关联用户表',
                             group_id BIGINT NOT NULL COMMENT '所属家庭组ID，关联family_group.id',
                             record_date DATE NOT NULL COMMENT '数据记录所属日期',
                             health_value VARCHAR(512) COMMENT '主要健康监测数据，JSON格式（如血压、血糖等）',
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据生成时间',
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
                             is_deleted BOOLEAN DEFAULT 0 COMMENT '0=正常，1=软删除',
                             INDEX idx_user_id (user_id),
                             INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭成员健康数据表';

-- ====================================
-- 7. 邀请/申请记录表
-- ====================================
CREATE TABLE family_invite_apply (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请/邀请记录唯一ID，自增主键',
                                     group_id BIGINT NOT NULL COMMENT '家庭组ID，关联family_group.id',
                                     inviter_id BIGINT NOT NULL COMMENT '发起方用户ID（组长邀请为组长，申请为申请人）',
                                     invitee_id BIGINT NOT NULL COMMENT '被邀请/申请用户ID',
                                     type VARCHAR(8) NOT NULL COMMENT '类型：invite=邀请，apply=申请',
                                     status VARCHAR(16) NOT NULL COMMENT '状态：pending=待处理，accepted=同意，rejected=拒绝，expired=过期，canceled=撤销',
                                     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     deal_time DATETIME DEFAULT NULL COMMENT '处理时间，未处理为NULL',
                                     expire_time DATETIME DEFAULT NULL COMMENT '过期时间（系统定义一般为48小时）',
                                     remark VARCHAR(128) DEFAULT NULL COMMENT '申请/邀请时的理由或说明',
                                     INDEX idx_group_id (group_id),
                                     INDEX idx_inviter_id (inviter_id),
                                     INDEX idx_invitee_id (invitee_id),
                                     INDEX idx_type_status (type, status),
                                     INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭组申请和邀请流水表';

-- ====================================
-- 8. 时间日志表
-- ====================================
CREATE TABLE family_event_log (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '事件流水ID，自增主键',
                                  group_id BIGINT NOT NULL COMMENT '家庭组ID，关联family_group.id',
                                  user_id BIGINT NOT NULL COMMENT '操作用户ID',
                                  event_type VARCHAR(32) NOT NULL COMMENT '事件类型：apply=申请，invite=邀请，approve=审批，quit=退出，medicine_alarm=用药异常，medicine_update=打卡变更',
                                  event_content VARCHAR(512) NOT NULL COMMENT '详细描述内容，通常为可读说明（如张三08:00漏服XX药）',
                                  event_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
                                  is_deleted BOOLEAN DEFAULT 0 COMMENT '0=正常，1=软删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭组所有事件日志表';