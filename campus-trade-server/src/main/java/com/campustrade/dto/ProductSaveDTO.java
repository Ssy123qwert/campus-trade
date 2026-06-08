package com.campustrade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发布/编辑商品请求 DTO
 */
@Data
public class ProductSaveDTO {
    private Long id; // 编辑时传入

    @NotNull(message = "卖家ID不能为空")
    private Long userId;

    @NotBlank(message = "商品标题不能为空")
    @Size(min = 1, max = 100, message = "标题长度需在1-100个字符之间")
    private String title;

    @Size(max = 2000, message = "描述长度不能超过2000个字符")
    private String description;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private BigDecimal originalPrice;
    private String category;
    private String images;
    private String videoUrl;
    private Integer status; // 商品状态 1在售 2已售 3已下架

    @NotNull(message = "成色不能为空")
    private Integer condition;
}
