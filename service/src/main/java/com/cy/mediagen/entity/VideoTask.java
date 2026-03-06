package com.cy.mediagen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频渲染任务实体（视频异步生成的生命周期）
 *
 * @author cy
 * @date 2026-03-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("video_task")
public class VideoTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联租户ID */
    private Long tenantId;

    /** 关联的人像底图资产ID */
    private Long sourceAssetId;

    /** 关联的门店背景资产ID（可为空） */
    private Long bgAssetId;

    /** 任务状态：PENDING-排队中，PROCESSING-渲染中，SUCCESS-成功，FAILED-失败 */
    private String taskStatus;

    /** 最终生成的视频 OSS 链接 */
    private String resultVideoUrl;

    /** 失败原因记录 */
    private String errorLog;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
