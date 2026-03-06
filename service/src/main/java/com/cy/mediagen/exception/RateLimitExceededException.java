package com.cy.mediagen.exception;

import java.io.Serial;

/**
 * 接口限流超限异常
 * <p>
 * 当租户在限定时间窗口内请求次数超过阈值时抛出。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
public class RateLimitExceededException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
