package com.cy.mediagen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cy.mediagen.entity.Tenant;

/**
 * 租户服务接口
 *
 * @author cy
 * @date 2026-03-06
 */
public interface TenantService extends IService<Tenant> {

    /**
     * 乐观锁扣减算力余额
     * <p>
     * 高并发安全：基于 MyBatis-Plus @Version 乐观锁机制，
     * 防止余额被超扣。若并发冲突（update 返回 0）将重试。
     * </p>
     *
     * @param tenantId 租户ID
     * @param points   扣减点数
     * @throws com.cy.mediagen.exception.InsufficientBalanceException 余额不足时抛出
     */
    void deductBalance(Long tenantId, int points);

    /**
     * 退还算力余额（异常补偿）
     * <p>
     * 当业务操作失败时，将预扣的算力点数退还给租户。
     * </p>
     *
     * @param tenantId 租户ID
     * @param points   退还点数
     */
    void refundBalance(Long tenantId, int points);
}
