create table t_user (
                        id bigint primary key auto_increment comment "自增主键",
                        username varchar(50) unique not null comment  "用户名 | 账号名",
                        password varchar(255) not null comment "密码",
                        nickname varchar(20) not null comment "昵称",
                        create_time datetime not null comment "创建时间",
                        update_time datetime not null comment "更新时间，但是应用不设计更新密码，所以其实没有用"
) engine = InnoDB default charset = utf8mb4 comment "用户表";