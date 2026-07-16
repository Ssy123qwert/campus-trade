-- ===== 通知表 =====
CREATE TABLE IF NOT EXISTS `t_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '接收用户',
  `type` VARCHAR(30) NOT NULL COMMENT '通知类型：NEW_MESSAGE / ORDER_STATUS / FAVORITE / OFFER',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT DEFAULT NULL COMMENT '通知内容',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联ID（订单ID/商品ID等）',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知表';
