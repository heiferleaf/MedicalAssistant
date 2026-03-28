create table if not exists agent_messages
(
    id         bigint auto_increment
    primary key,
    session_id varchar(64)                        not null,
    user_id    varchar(64)                        not null,
    role       varchar(32)                        not null comment 'user|assistant|tool',
    content    text                               not null,
    created_at datetime default CURRENT_TIMESTAMP not null
    )
    comment 'agent 消息表' collate = utf8mb4_unicode_ci;

create index idx_agent_messages_session_id_id
    on agent_messages (session_id, id);

create index idx_agent_messages_user_id
    on agent_messages (user_id);

create table if not exists agent_pending_actions
(
    action_id      varchar(64)                        not null
    primary key,
    session_id     varchar(64)                        not null,
    user_id        varchar(64)                        not null,
    action_type    varchar(64)                        not null,
    preview_json   text                               not null,
    tool_args_json text                               not null,
    status         varchar(16)                        not null comment 'pending|done|failed|canceled',
    result_json    text                               null,
    created_at     datetime default CURRENT_TIMESTAMP not null,
    expires_at     datetime                           not null
    )
    comment 'agent 待确认动作表' collate = utf8mb4_unicode_ci;

create index idx_agent_pending_actions_expires_at
    on agent_pending_actions (expires_at);

create index idx_agent_pending_actions_session_id
    on agent_pending_actions (session_id);

create index idx_agent_pending_actions_user_id_status
    on agent_pending_actions (user_id, status);

create table if not exists agent_sessions
(
    session_id   varchar(64)                        not null
    primary key,
    user_id      varchar(64)                        not null,
    created_at   datetime default CURRENT_TIMESTAMP not null,
    updated_at   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    summary_text text                               null
    )
    comment 'agent 会话表' collate = utf8mb4_unicode_ci;

create index idx_agent_sessions_updated_at
    on agent_sessions (updated_at);

create index idx_agent_sessions_user_id
    on agent_sessions (user_id);

create table if not exists family_event_log
(
    id            bigint auto_increment comment '事件流水ID，自增主键'
    primary key,
    group_id      bigint                               not null comment '家庭组ID，关联family_group.id',
    user_id       bigint                               not null comment '操作用户ID',
    event_type    varchar(32)                          not null comment '事件类型：medicine_alarm=用药异常，task_done=正常打卡 info:其他消息，如用户加入家庭组、退出家庭组、创建家庭组、加入家庭组被拒绝',
    event_content varchar(512)                         not null comment '详细描述内容，通常为可读说明（如张三08:00漏服XX药）',
    event_time    datetime   default CURRENT_TIMESTAMP not null comment '事件发生时间',
    is_deleted    tinyint(1) default 0                 null comment '0=正常，1=软删除'
    )
    comment '家庭组所有事件日志表';

create table if not exists family_group
(
    id            bigint auto_increment comment '家庭组唯一ID，自增主键'
    primary key,
    group_name    varchar(64)                          not null comment '家庭组名称',
    owner_user_id bigint                               not null comment '组长用户ID',
    description   varchar(256)                         null comment '家庭组简介或补充说明',
    create_time   datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '最后更新时间',
    is_deleted    tinyint(1) default 0                 null comment '0=正常，1=软删除（逻辑删除）'
    )
    comment '家庭组表';

create table if not exists family_invite_apply
(
    id          bigint auto_increment comment '申请/邀请记录唯一ID，自增主键'
    primary key,
    group_id    bigint                             not null comment '家庭组ID，关联family_group.id',
    inviter_id  bigint                             null comment '发起方用户ID（组长邀请为组长，申请为申请人）',
    invitee_id  bigint                             not null comment '被邀请/申请用户ID',
    type        varchar(8)                         not null comment '类型：invite=邀请，apply=申请',
    status      varchar(16)                        not null comment '状态：pending=待处理，accepted=同意，rejected=拒绝，expired=过期，canceled=撤销',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    deal_time   datetime                           null comment '处理时间，未处理为NULL',
    expire_time datetime                           null comment '过期时间（系统定义一般为48小时）',
    remark      varchar(128)                       null comment '申请/邀请时的理由或说明'
    )
    comment '家庭组申请和邀请流水表';

create index idx_expire_time
    on family_invite_apply (expire_time);

create index idx_group_id
    on family_invite_apply (group_id);

create index idx_invitee_id
    on family_invite_apply (invitee_id);

create index idx_inviter_id
    on family_invite_apply (inviter_id);

create index idx_type_status
    on family_invite_apply (type, status);

create table if not exists family_member
(
    id         bigint auto_increment comment '家庭组成员唯一ID，自增主键'
    primary key,
    group_id   bigint               not null comment '所属家庭组ID，关联family_group.id',
    user_id    bigint               not null comment '用户ID',
    role       varchar(16)          not null comment '成员角色，leader=组长，normal=普通成员',
    join_time  datetime             not null comment '加入家庭组时间',
    status     varchar(16)          not null comment '成员状态：active=在组，quit=退出',
    exit_time  datetime             null comment '退出时间，未退出为NULL',
    is_deleted tinyint(1) default 0 null comment '0=正常，1=软删除（逻辑删除）'
    )
    comment '家庭组成员表';

create index idx_group_user
    on family_member (group_id, user_id);

create table if not exists health_data
(
    id             bigint auto_increment comment '自增主键'
    primary key,
    user_id        bigint                             not null comment '用户ID',
    heart_rate     double                             null comment '平均心率',
    blood_pressure double                             null comment '平均血压',
    measure_time   datetime                           not null comment '测量时间',
    created_at     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    is_deleted     tinyint  default 0                 null comment '逻辑删除'
)
    comment '健康数据表';

create index idx_measure_time
    on health_data (measure_time);

create index idx_user_id
    on health_data (user_id);

create table if not exists medicine
(
    id             bigint auto_increment comment '药品ID'
    primary key,
    user_id        bigint                             not null comment '所属用户ID',
    name           varchar(100)                       not null comment '药品名称',
    default_dosage varchar(50)                        null comment '推荐剂量（可选）',
    remark         varchar(255)                       null comment '备注',
    created_at     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
    )
    comment '药品表' collate = utf8mb4_unicode_ci;

create table if not exists medication_plan
(
    id          bigint auto_increment comment '计划ID'
    primary key,
    user_id     bigint                             not null comment '所属用户ID',
    medicine_id bigint                             not null comment '药品ID',
    dosage      varchar(50)                        not null comment '本次计划剂量',
    start_date  date                               not null comment '开始日期',
    end_date    date                               null comment '结束日期（可空表示长期）',
    time_points varchar(100)                       not null comment '每日服药时间点JSON数组，如["08:00","20:00"]',
    remark      varchar(255)                       null comment '备注',
    deleted     tinyint  default 0                 not null comment '软删除标识：0-正常，1-已删除',
    created_at  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint medication_plan_ibfk_1
    foreign key (medicine_id) references medicine (id)
    )
    comment '用药计划表' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on medication_plan (deleted);

create index idx_user_id
    on medication_plan (user_id);

create index medicine_id
    on medication_plan (medicine_id);

create table if not exists medication_task
(
    id           bigint auto_increment comment '任务ID'
    primary key,
    user_id      bigint                             not null comment '所属用户ID',
    plan_id      bigint                             not null comment '归属计划ID',
    medicine_id  bigint                             not null comment '药品ID（冗余字段）',
    dosage       varchar(50)                        not null comment '剂量（冗余字段）',
    task_date    date                               not null comment '任务日期',
    time_point   varchar(10)                        not null comment '服药时间点，如"08:00"',
    status       tinyint  default 0                 not null comment '状态：0-未服用，1-已服用，2-漏服',
    operate_time datetime                           null comment '操作时间（标记状态的时间）',
    created_at   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint medication_task_ibfk_1
    foreign key (plan_id) references medication_plan (id),
    constraint medication_task_ibfk_2
    foreign key (medicine_id) references medicine (id)
    )
    comment '用药任务表' collate = utf8mb4_unicode_ci;

create index idx_plan_id
    on medication_task (plan_id);

create index idx_status
    on medication_task (status);

create index idx_user_task_date
    on medication_task (user_id, task_date);

create index medicine_id
    on medication_task (medicine_id);

create index idx_user_id
    on medicine (user_id);

create table if not exists t_user
(
    id           bigint auto_increment comment '自增主键'
    primary key,
    username     varchar(50)  not null comment '用户名 | 账号名',
    password     varchar(255) not null comment '密码',
    nickname     varchar(20)  not null comment '昵称',
    create_time  datetime     not null comment '创建时间',
    update_time  datetime     not null comment '更新时间，但是应用不设计更新密码，所以其实没有用',
    phone_number varchar(11)  not null comment '手机号',
    constraint phoneNumber
    unique (phone_number),
    constraint username
    unique (username)
    )
    comment '用户表';

