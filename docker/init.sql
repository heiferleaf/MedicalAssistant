-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: medicalassistant
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `agent_messages`
--

CREATE USER 'canal'@'%' IDENTIFIED WITH mysql_native_password BY 'canal_password';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;

DROP TABLE IF EXISTS `agent_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_messages` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `role` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'user|assistant|tool',
                                  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `action_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'plan, medicine, task, family ',
                                  `action_data` text COLLATE utf8mb4_unicode_ci COMMENT 'JSON ',
                                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`),
                                  KEY `idx_agent_messages_session_id_id` (`session_id`,`id`),
                                  KEY `idx_agent_messages_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=300 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='agent 消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agent_pending_actions`
--

DROP TABLE IF EXISTS `agent_pending_actions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_pending_actions` (
                                         `action_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `action_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `preview_json` text COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `tool_args_json` text COLLATE utf8mb4_unicode_ci NOT NULL,
                                         `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'pending|done|failed|canceled',
                                         `result_json` text COLLATE utf8mb4_unicode_ci,
                                         `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         `expires_at` datetime NOT NULL,
                                         PRIMARY KEY (`action_id`),
                                         KEY `idx_agent_pending_actions_expires_at` (`expires_at`),
                                         KEY `idx_agent_pending_actions_session_id` (`session_id`),
                                         KEY `idx_agent_pending_actions_user_id_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='agent 待确认动作表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agent_sessions`
--

DROP TABLE IF EXISTS `agent_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_sessions` (
                                  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  `summary_text` text COLLATE utf8mb4_unicode_ci,
                                  PRIMARY KEY (`session_id`),
                                  KEY `idx_agent_sessions_updated_at` (`updated_at`),
                                  KEY `idx_agent_sessions_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='agent 会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `family_event_log`
--

DROP TABLE IF EXISTS `family_event_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `family_event_log` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件流水ID，自增主键',
                                    `group_id` bigint NOT NULL COMMENT '家庭组ID，关联family_group.id',
                                    `user_id` bigint NOT NULL COMMENT '操作用户ID',
                                    `event_type` varchar(32) NOT NULL COMMENT '事件类型：apply=申请，invite=邀请，approve=审批，quit=退出，medicine_alarm=用药异常，medicine_update=打卡变更',
                                    `event_content` varchar(512) NOT NULL COMMENT '详细描述内容，通常为可读说明（如张三08:00漏服XX药）',
                                    `event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
                                    `is_deleted` tinyint(1) DEFAULT '0' COMMENT '0=正常，1=软删除',
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=216 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭组所有事件日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `family_group`
--

DROP TABLE IF EXISTS `family_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `family_group` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '家庭组唯一ID，自增主键',
                                `group_name` varchar(64) NOT NULL COMMENT '家庭组名称',
                                `owner_user_id` bigint NOT NULL COMMENT '组长用户ID',
                                `description` varchar(256) DEFAULT NULL COMMENT '家庭组简介或补充说明',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
                                `is_deleted` tinyint(1) DEFAULT '0' COMMENT '0=正常，1=软删除（逻辑删除）',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `family_invite_apply`
--

DROP TABLE IF EXISTS `family_invite_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `family_invite_apply` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请/邀请记录唯一ID，自增主键',
                                       `group_id` bigint NOT NULL COMMENT '家庭组ID，关联family_group.id',
                                       `inviter_id` bigint DEFAULT NULL COMMENT '发起方用户ID（组长邀请为组长，申请为申请人）',
                                       `invitee_id` bigint NOT NULL COMMENT '被邀请/申请用户ID',
                                       `type` varchar(8) NOT NULL COMMENT '类型：invite=邀请，apply=申请',
                                       `status` varchar(16) NOT NULL COMMENT '状态：pending=待处理，accepted=同意，rejected=拒绝，expired=过期，canceled=撤销',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `deal_time` datetime DEFAULT NULL COMMENT '处理时间，未处理为NULL',
                                       `expire_time` datetime DEFAULT NULL COMMENT '过期时间（系统定义一般为48小时）',
                                       `remark` varchar(128) DEFAULT NULL COMMENT '申请/邀请时的理由或说明',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_expire_time` (`expire_time`),
                                       KEY `idx_group_id` (`group_id`),
                                       KEY `idx_invitee_id` (`invitee_id`),
                                       KEY `idx_inviter_id` (`inviter_id`),
                                       KEY `idx_type_status` (`type`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭组申请和邀请流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `family_member`
--

DROP TABLE IF EXISTS `family_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `family_member` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '家庭组成员唯一ID，自增主键',
                                 `group_id` bigint NOT NULL COMMENT '所属家庭组ID，关联family_group.id',
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `role` varchar(16) NOT NULL COMMENT '成员角色，leader=组长，normal=普通成员',
                                 `join_time` datetime NOT NULL COMMENT '加入家庭组时间',
                                 `status` varchar(16) NOT NULL COMMENT '成员状态：active=在组，quit=退出',
                                 `exit_time` datetime DEFAULT NULL COMMENT '退出时间，未退出为NULL',
                                 `is_deleted` tinyint(1) DEFAULT '0' COMMENT '0=正常，1=软删除（逻辑删除）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_group_user` (`group_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭组成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `health_data`
--

DROP TABLE IF EXISTS `health_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `health_data` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `user_id` bigint NOT NULL COMMENT '用户ID',
                               `heart_rate` double DEFAULT NULL,
                               `step_count` int DEFAULT NULL,
                               `sleep_duration` double DEFAULT NULL,
                               `sleep_scope` int DEFAULT NULL COMMENT '睡眠评分0-100',
                               `bloodOxygen` double DEFAULT NULL,
                               `relax_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                               `relax_sub_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '放松子类型',
                               `relax_duration` double DEFAULT NULL,
                               `pressure_max_score` int DEFAULT NULL COMMENT '压力最大评分0-100',
                               `pressure_min_score` int DEFAULT NULL COMMENT '压力最小评分0-100',
                               `pressure_avg_score` int DEFAULT NULL COMMENT '压力平均评分0-100',
                               `measure_time` datetime NOT NULL,
                               `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `is_deleted` tinyint DEFAULT '0',
                               PRIMARY KEY (`id`),
                               KEY `idx_user_id` (`user_id`),
                               KEY `idx_measure_time` (`measure_time`),
                               KEY `idx_user_measure_time` (`user_id`,`measure_time`),
                               KEY `idx_is_deleted` (`is_deleted`),
                               CONSTRAINT `fk_health_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medication_plan`
--

DROP TABLE IF EXISTS `medication_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medication_plan` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
                                   `user_id` bigint NOT NULL COMMENT '所属用户ID',
                                   `medicine_id` bigint NOT NULL COMMENT '药品ID',
                                   `dosage` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '本次计划剂量',
                                   `start_date` date NOT NULL COMMENT '开始日期',
                                   `end_date` date DEFAULT NULL COMMENT '结束日期（可空表示长期）',
                                   `time_points` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '每日服药时间点JSON数组，如["08:00","20:00"]',
                                   `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                   `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '软删除标识：0-正常，1-已删除',
                                   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_deleted` (`deleted`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `medicine_id` (`medicine_id`),
                                   CONSTRAINT `medication_plan_ibfk_1` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=124 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medication_task`
--

DROP TABLE IF EXISTS `medication_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medication_task` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                                   `user_id` bigint NOT NULL COMMENT '所属用户ID',
                                   `plan_id` bigint NOT NULL COMMENT '归属计划ID',
                                   `medicine_id` bigint NOT NULL COMMENT '药品ID（冗余字段）',
                                   `dosage` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '剂量（冗余字段）',
                                   `task_date` date NOT NULL COMMENT '任务日期',
                                   `time_point` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服药时间点，如"08:00"',
                                   `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-未服用，1-已服用，2-漏服',
                                   `operate_time` datetime DEFAULT NULL COMMENT '操作时间（标记状态的时间）',
                                   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_plan_id` (`plan_id`),
                                   KEY `idx_status` (`status`),
                                   KEY `idx_user_task_date` (`user_id`,`task_date`),
                                   KEY `medicine_id` (`medicine_id`),
                                   CONSTRAINT `medication_task_ibfk_1` FOREIGN KEY (`plan_id`) REFERENCES `medication_plan` (`id`),
                                   CONSTRAINT `medication_task_ibfk_2` FOREIGN KEY (`medicine_id`) REFERENCES `medicine` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medicine`
--

DROP TABLE IF EXISTS `medicine`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medicine` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '药品ID',
                            `user_id` bigint NOT NULL COMMENT '所属用户ID',
                            `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '药品名称',
                            `default_dosage` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推荐剂量（可选）',
                            `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                            `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `deleted` tinyint NOT NULL DEFAULT '0',
                            PRIMARY KEY (`id`),
                            KEY `idx_user_id` (`user_id`),
                            KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=118 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_user`
--

DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
                          `username` varchar(50) NOT NULL COMMENT '用户名 | 账号名',
                          `password` varchar(255) NOT NULL COMMENT '密码',
                          `nickname` varchar(20) NOT NULL COMMENT '昵称',
                          `create_time` datetime NOT NULL COMMENT '创建时间',
                          `update_time` datetime NOT NULL COMMENT '更新时间，但是应用不设计更新密码，所以其实没有用',
                          `phone_number` varchar(11) NOT NULL COMMENT '手机号',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `phoneNumber` (`phone_number`),
                          UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tool_execution_pending`
--

DROP TABLE IF EXISTS `tool_execution_pending`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tool_execution_pending` (
                                          `request_id` varchar(64) NOT NULL COMMENT 'Tool请求ID',
                                          `user_id` bigint NOT NULL COMMENT '用户ID',
                                          `session_id` varchar(64) NOT NULL COMMENT '会话ID',
                                          `tool_name` varchar(128) NOT NULL COMMENT 'Tool名称，如 createPlan, updatePlan, deletePlan',
                                          `tool_arguments` text NOT NULL COMMENT 'Tool参数 JSON',
                                          `original_ai_message` text COMMENT 'AI原始消息',
                                          `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, EXECUTED, EXPIRED',
                                          `edited_data` text COMMENT '编辑后的数据 JSON',
                                          `executed_at` datetime DEFAULT NULL,
                                          `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          `expires_at` datetime NOT NULL COMMENT '过期时间（30分钟）',
                                          PRIMARY KEY (`request_id`),
                                          KEY `idx_user_id` (`user_id`),
                                          KEY `idx_session_id` (`session_id`),
                                          KEY `idx_status` (`status`),
                                          KEY `idx_created_at` (`created_at`),
                                          KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tool授权记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-13  7:06:12
