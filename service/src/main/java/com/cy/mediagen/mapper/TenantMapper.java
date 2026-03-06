package com.cy.mediagen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cy.mediagen.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 Mapper 接口
 *
 * @author cy
 * @date 2026-03-06
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
