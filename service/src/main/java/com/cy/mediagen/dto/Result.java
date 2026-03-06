package com.cy.mediagen.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全局统一响应体
 *
 * @param <T> 数据泛型
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 数据 */
    private T data;

    /** 时间戳 */
    private String timestamp;

    /**
     * 成功（带数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 成功（无数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功（自定义消息 + 数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * 失败
     */
    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
