package com.cy.mediagen.exception;

import com.cy.mediagen.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理业务异常，使用 Result 标准化响应格式。
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
    public ResponseEntity<Result<Void>> handleInsufficientBalance(InsufficientBalanceException e) {
        log.warn("算力余额不足: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Result.fail(402, "算力余额不足，请充值"));
    }

    /**
     * 处理接口限流异常 → HTTP 429 Too Many Requests
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Result<Void>> handleRateLimitExceeded(RateLimitExceededException e) {
        log.warn("接口限流触发: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Result.fail(429, "操作过于频繁，请稍后再试"));
    }

    /**
     * 处理 @Valid 参数校验异常 → HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(400, detail));
    }

    /**
     * 处理参数校验异常 → HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(400, e.getMessage()));
    }

    /**
     * 处理非法状态异常 → HTTP 403 Forbidden
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> handleIllegalState(IllegalStateException e) {
        log.warn("非法状态: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(403, e.getMessage()));
    }

    /**
     * 兜底处理未知异常 → HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUncaughtException(Exception e) {
        log.error("系统内部错误: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "系统繁忙，请稍后重试"));
    }
}
