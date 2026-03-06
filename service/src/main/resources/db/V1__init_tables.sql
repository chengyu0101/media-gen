-- ============================================================
-- 医美短视频生成 SaaS 平台 - 核心表 DDL
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- ============================================================

-- -----------------------------------------------------------
-- 1. 租户表（B 端商家信息）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_name` VARCHAR(128) NOT NULL COMMENT '机构名称',
    `industry_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '行业类型（如：美业、KTV、口腔）',
    `balance_points` INT NOT NULL DEFAULT 0 COMMENT '剩余算力点数（核心计费字段）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_industry_type` (`industry_type`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '租户表（B 端商家信息）';

-- -----------------------------------------------------------
-- 2. 数字资产库表（用户上传的高清底图、背景图等原始资产）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `asset_library` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '关联租户ID',
    `asset_type` VARCHAR(32) NOT NULL COMMENT '资产类型：AVATAR_IMAGE-人像底图，BG_IMAGE-背景图，AUDIO-音频',
    `object_key` VARCHAR(512) NOT NULL COMMENT 'OSS 对象唯一标识 Key',
    `asset_url` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '资产访问外链地址',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_asset_type` (`asset_type`),
    UNIQUE INDEX `uk_object_key` (`object_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数字资产库表（原始素材存储）';

-- -----------------------------------------------------------
-- 3. 视频渲染任务表（视频异步生成的生命周期）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `video_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` BIGINT NOT NULL COMMENT '关联租户ID',
    `source_asset_id` BIGINT NOT NULL COMMENT '关联的人像底图资产ID',
    `bg_asset_id` BIGINT NULL DEFAULT NULL COMMENT '关联的门店背景资产ID（可为空）',
    `task_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-排队中，PROCESSING-渲染中，SUCCESS-成功，FAILED-失败',
    `result_video_url` VARCHAR(1024) NULL DEFAULT NULL COMMENT '最终生成的视频 OSS 链接',
    `error_log` TEXT NULL COMMENT '失败原因记录',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_id` (`tenant_id`),
    INDEX `idx_task_status` (`task_status`),
    INDEX `idx_source_asset_id` (`source_asset_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '视频渲染任务表（异步生成生命周期）';

-- -----------------------------------------------------------
-- 增量 DDL：为租户表添加乐观锁版本号字段（如已建表则执行此语句）
-- -----------------------------------------------------------
-- ALTER TABLE `tenant` ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号' AFTER `status`;