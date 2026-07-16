package com.campustrade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    private Integer status;  // 0待支付 1已支付 2已发货 3已完成 4已取消
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
}
