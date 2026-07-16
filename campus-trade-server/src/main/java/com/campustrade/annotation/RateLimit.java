package com.campustrade.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 *
 * 用法：@RateLimit(key = "login", max = 10, window = 60)
 * 表示 60 秒内同一 IP 最多请求 10 次
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流 Key（同组接口共享计数） */
    String key();

    /** 窗口内最大请求数 */
    int max() default 10;

    /** 时间窗口（秒） */
    int window() default 60;
}
