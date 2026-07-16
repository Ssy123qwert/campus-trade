package com.campustrade.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Elasticsearch 商品索引文档
 *
 * 用于全文检索，支持 IK 中文分词
 * 索引名：campus_product
 *
 * 注意：ES 不可用时，搜索会自动降级到 MySQL LIKE 查询
 */
@Data
@Document(indexName = "campus_product")
public class ProductDocument {

    @Id
    private Long id;

    /** 商品标题（IK 中文分词，权重 3.0） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /** 商品描述（IK 中文分词，权重 1.0） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /** 分类（不分词，精确匹配） */
    @Field(type = FieldType.Keyword)
    private String category;

    /** 价格 */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /** 成色 1-5 */
    @Field(type = FieldType.Integer)
    private Integer condition;

    /** 状态：1=在售 2=已预定 3=已下架 */
    @Field(type = FieldType.Integer)
    private Integer status;

    /** 浏览数 */
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    /** 卖家 ID */
    @Field(type = FieldType.Long)
    private Long userId;

    /** 发布时间 */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
}
