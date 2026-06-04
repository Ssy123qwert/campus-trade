package com.campustrade.dto;

import lombok.Data;

@Data
public class ProductQuery {
    private String keyword;
    private String category;
    private Integer condition;
    private String sort;  // price_asc, price_desc, time_desc
    private Integer page = 1;
    private Integer size = 10;
}
