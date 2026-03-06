package com.cy.mediagen.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * <p>
 * 统一处理业务异常，返回标准化的错误响应。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理算力余额不足异常 → HTTP 402 Payment Required
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException e) {
        log.warn("算力余额不足: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_BALANCE", e.getMessage());
    }

    /**
     * 处理接口限流异常 → HTTP 429 Too Many Requests
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException e) {
        log.warn("接口限流触发: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", e.getMessage());
    }

    /**
     * 处理参数校验异常 → HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", e.getMessage());
    }

    /**
     * 处理非法状态异常 → HTTP 403 Forbidden
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("非法状态: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "ILLEGAL_STATE", e.getMessage());
    }

    /**
     * 兜底处理未知异常 → HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUncaughtException(Exception e) {
        log.error("系统内部错误: {}", e.getMessage(), e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系统繁忙，请稍后重试");
    }

    /**
     * 构建标准化错误响应
     *
     * @param status  HTTP 状态码
     * @param code    业务错误码
     * @param message 错误消息
     * @return 错误响应
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String code, String message) {
        Map<String, Object> body = new HashMap<>(4);
        body.put("code", code);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        return ResponseEntity.status(status).body(body);
    }
}
