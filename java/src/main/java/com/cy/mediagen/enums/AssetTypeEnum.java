package com.cy.mediagen.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数字资产类型枚举
 *
 * @author cy
 * @date 2026-03-06
 */
@Getter
@AllArgsConstructor
public enum AssetTypeEnum {

    /** 人像底图 */
    AVATAR_IMAGE("AVATAR_IMAGE", "人像底图"),

    /** 门店背景图 */
    BG_IMAGE("BG_IMAGE", "背景图"),

    /** 音频素材 */
    AUDIO("AUDIO", "音频");

    /** 类型编码 */
    private final String code;

    /** 中文描述 */
    private final String description;
}
