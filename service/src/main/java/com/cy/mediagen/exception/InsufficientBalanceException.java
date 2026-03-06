package com.cy.mediagen.exception;

import java.io.Serial;

/**
 * 算力余额不足异常
 * <p>
 * 当租户算力点数不足以支付本次操作时抛出。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
public class InsufficientBalanceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
