package com.campustrade.exception;

import lombok.Getter;

/**
 * 业务异常类，用于替代 Service 层返回 null 的模式。
 * 不同业务场景使用不同的错误码，方便前端精确处理。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** HTTP 风格错误码：400-参数错误, 401-未授权, 403-禁止操作, 404-资源不存在, 409-冲突, 500-服务异常 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    // ──── 便捷工厂方法 ────

    public static BusinessException badRequest(String msg) {
        return new BusinessException(400, msg);
    }

    public static BusinessException notFound(String msg) {
        return new BusinessException(404, msg);
    }

    public static BusinessException conflict(String msg) {
        return new BusinessException(409, msg);
    }

    public static BusinessException forbidden(String msg) {
        return new BusinessException(403, msg);
    }

    public static BusinessException serverError(String msg) {
        return new BusinessException(500, msg);
    }
}
