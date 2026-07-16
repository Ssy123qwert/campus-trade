-- ===== 议价表（砍价） =====
CREATE TABLE IF NOT EXISTS `t_offer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `buyer_id` BIGINT NOT NULL COMMENT '出价人',
  `seller_id` BIGINT NOT NULL COMMENT '卖家',
  `price` DECIMAL(10,2) NOT NULL COMMENT '出价金额',
  `status` TINYINT DEFAULT 0 COMMENT '0=待回复 1=已接受 2=已拒绝 3=已还价',
  `reply_price` DECIMAL(10,2) DEFAULT NULL COMMENT '卖家还价金额',
  `reply_msg` VARCHAR(500) DEFAULT NULL COMMENT '回复留言',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_buyer` (`buyer_id`),
  KEY `idx_seller` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='议价表';

-- ===== 举报表 =====
CREATE TABLE IF NOT EXISTS `t_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reporter_id` BIGINT NOT NULL COMMENT '举报人',
  `target_type` VARCHAR(20) NOT NULL COMMENT '举报类型：PRODUCT / USER / MESSAGE',
  `target_id` BIGINT NOT NULL COMMENT '被举报对象ID',
  `reason` VARCHAR(50) NOT NULL COMMENT '举报原因',
  `description` TEXT DEFAULT NULL COMMENT '详细描述',
  `status` TINYINT DEFAULT 0 COMMENT '0=待处理 1=已处理 2=已驳回',
  `handle_msg` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_reporter` (`reporter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报表';
