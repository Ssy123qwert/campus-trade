-- ============================================================
-- 校园二手交易平台 — 数据库升级脚本 V1
-- 用于在现有 init.sql 基础上新增字段和表
-- 执行：mysql -u root -p campus_trade < V1__upgrade.sql
-- ============================================================

USE `campus_trade`;

-- ===== 辅助：用存储过程安全添加字段 =====
-- 避免因字段已存在导致脚本中断
DELIMITER ;;
DROP PROCEDURE IF EXISTS `add_col`;;
CREATE PROCEDURE `add_col`(
  IN p_table VARCHAR(64),
  IN p_col VARCHAR(64),
  IN p_def VARCHAR(512)
)
BEGIN
  DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; -- Duplicate column → 跳过
  SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_col, ' ', p_def);
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
END;;
DELIMITER ;

-- ===== 1. t_user 表升级 =====
CALL add_col('t_user', 'email',           'VARCHAR(100) DEFAULT NULL COMMENT ''电子邮箱'' AFTER `phone`');
CALL add_col('t_user', 'total_rating',    'INT NOT NULL DEFAULT 0 COMMENT ''卖家总评分'' AFTER `role`');
CALL add_col('t_user', 'rating_count',    'INT NOT NULL DEFAULT 0 COMMENT ''卖家评价数'' AFTER `total_rating`');

-- 把现有的 root 用户设为管理员
UPDATE `t_user` SET `role` = 1 WHERE `username` = 'root' AND (`role` IS NULL OR `role` = 0);

-- ===== 2. t_order 表升级 =====
-- 状态：0=待付款 1=已支付 2=已发货 3=已完成 4=已取消
CALL add_col('t_order', 'order_no',       'VARCHAR(32) DEFAULT NULL COMMENT ''唯一订单号'' AFTER `id`');
CALL add_col('t_order', 'update_time',    'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `create_time`');
CALL add_col('t_order', 'pay_time',       'DATETIME DEFAULT NULL COMMENT ''支付时间'' AFTER `update_time`');
CALL add_col('t_order', 'cancel_time',    'DATETIME DEFAULT NULL COMMENT ''取消时间'' AFTER `pay_time`');

-- 生成已有订单的 order_no
UPDATE `t_order` SET `order_no` = CONCAT('ORD', DATE_FORMAT(`create_time`, '%Y%m%d%H%i%s'), LPAD(`id`, 6, '0')) WHERE `order_no` IS NULL;

-- ===== 3. t_product 表升级 =====
CALL add_col('t_product', 'version',      'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `status`');

-- ===== 清理辅助存储过程 =====
DROP PROCEDURE IF EXISTS `add_col`;

-- ===== 4. 创建 t_review 评价表 =====
CREATE TABLE IF NOT EXISTS `t_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '关联订单',
  `from_user_id` BIGINT NOT NULL COMMENT '评价人',
  `to_user_id` BIGINT NOT NULL COMMENT '被评价人',
  `rating` INT NOT NULL DEFAULT '5' COMMENT '评分 1-5',
  `content` TEXT DEFAULT NULL COMMENT '评价内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_to_user` (`to_user_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_from_user` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易评价表';

-- ===== 5. 创建 t_order_snapshot 订单快照表 =====
CREATE TABLE IF NOT EXISTS `t_order_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '关联订单',
  `product_title` VARCHAR(200) NOT NULL COMMENT '商品标题（快照）',
  `product_description` TEXT DEFAULT NULL COMMENT '商品描述（快照）',
  `product_price` DECIMAL(10,2) NOT NULL COMMENT '商品价格（快照）',
  `product_images` TEXT DEFAULT NULL COMMENT '商品图片（快照，JSON数组）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单快照表';

-- ===== 6. 创建 t_chat_message 聊天消息扩展表 =====
CREATE TABLE IF NOT EXISTS `t_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_user_id` BIGINT NOT NULL COMMENT '发送者',
  `to_user_id` BIGINT NOT NULL COMMENT '接收者',
  `product_id` BIGINT DEFAULT NULL COMMENT '关联商品',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `message_type` TINYINT DEFAULT 0 COMMENT '消息类型：0=TEXT 1=IMAGE 2=SYSTEM',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读：0=未读 1=已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_to` (`from_user_id`, `to_user_id`),
  KEY `idx_to_user_unread` (`to_user_id`, `is_read`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- ===== 7. 创建 t_user_behavior_log 行为日志表 =====
CREATE TABLE IF NOT EXISTS `t_user_behavior_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（可为空，匿名访问）',
  `action_type` VARCHAR(20) NOT NULL COMMENT '行为类型：VIEW/FAVORITE/SEARCH',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID（商品ID等）',
  `extra` VARCHAR(500) DEFAULT NULL COMMENT '额外信息（搜索关键词等）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户行为日志表';

-- ===== 8. 创建 t_operation_log 操作日志表 =====
CREATE TABLE IF NOT EXISTS `t_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
  `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作类型',
  `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  `params` TEXT DEFAULT NULL COMMENT '请求参数（JSON）',
  `result` VARCHAR(20) DEFAULT NULL COMMENT '结果：SUCCESS / FAIL',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表（审计追溯）';
