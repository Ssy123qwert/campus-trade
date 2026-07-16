package com.campustrade.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在 Controller 方法上，自动记录操作日志到 t_operation_log 表
 *
 * 用法示例：
 * @OperationLog("删除用户")
 * @DeleteMapping("/user")
 * public R<String> deleteUser(@RequestParam Long userId) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 操作描述，如 "删除用户"、"下架商品" */
    String value();
}
