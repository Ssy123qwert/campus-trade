package com.campustrade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Long productId;
    private String content;
    private Integer isRead;  // 0未读 1已读
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
