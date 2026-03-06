package com.cy.mediagen.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 视频渲染任务状态枚举
 *
 * @author cy
 * @date 2026-03-06
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    /** 排队中 */
    PENDING("PENDING", "排队中"),

    /** 渲染中 */
    PROCESSING("PROCESSING", "渲染中"),

    /** 渲染成功 */
    SUCCESS("SUCCESS", "成功"),

    /** 渲染失败 */
    FAILED("FAILED", "失败");

    /** 状态编码 */
    private final String code;

    /** 中文描述 */
    private final String description;
}
