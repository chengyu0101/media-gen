package com.cy.mediagen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cy.mediagen.entity.VideoTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频渲染任务 Mapper 接口
 *
 * @author cy
 * @date 2026-03-06
 */
@Mapper
public interface VideoTaskMapper extends BaseMapper<VideoTask> {
}
