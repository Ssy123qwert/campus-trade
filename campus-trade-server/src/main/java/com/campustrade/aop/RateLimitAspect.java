package com.campustrade.aop;

import com.campustrade.annotation.RateLimit;
import com.campustrade.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面
 *
 * 基于 Redis 的滑动窗口计数器
 * Key 格式：rate_limit:{key}:{ip}
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        String ip = getClientIp(request);
        String redisKey = "rate_limit:" + rateLimit.key() + ":" + ip;
        int max = rateLimit.max();
        int window = rateLimit.window();

        // Redis INCR + TTL
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, window, TimeUnit.SECONDS);
        }

        if (count != null && count > max) {
            throw BusinessException.badRequest("请求太频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
