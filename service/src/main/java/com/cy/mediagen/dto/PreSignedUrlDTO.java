package com.cy.mediagen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * OSS 预签名上传结果 DTO
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreSignedUrlDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预签名上传 URL（客户端使用 HTTP PUT 直传阿里云 OSS） */
    private String uploadUrl;

    /** OSS 对象唯一标识 Key（上传完成后用于入库记录） */
    private String objectKey;
}
