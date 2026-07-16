package com.campustrade.aop;

import com.campustrade.annotation.OperationLog;
import com.campustrade.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 * 拦截带有 @OperationLog 注解的 Controller 方法，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SecurityUtil securityUtil;
    private final JdbcTemplate jdbcTemplate;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // 获取请求信息
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        String operation = operationLog.value();
        Long userId = securityUtil.getCurrentUserId();
        String username = "";
        if (userId != null) {
            var user = securityUtil.getCurrentUserEntity();
            if (user != null) username = user.getUsername();
        }

        String method = joinPoint.getSignature().toShortString();
        String ip = request.getRemoteAddr();
        String params = request.getQueryString() != null ? request.getQueryString() : "";

        String result = "SUCCESS";
        try {
            Object ret = joinPoint.proceed();
            return ret;
        } catch (Exception e) {
            result = "FAIL";
            throw e;
        } finally {
            try {
                jdbcTemplate.update(
                        "INSERT INTO t_operation_log(user_id, username, operation, method, params, result, ip, create_time) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        userId, username, operation, method, params, result, ip, LocalDateTime.now()
                );
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
    }
}
