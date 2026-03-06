package com.cy.mediagen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.mediagen.entity.Tenant;
import com.cy.mediagen.exception.InsufficientBalanceException;
import com.cy.mediagen.mapper.TenantMapper;
import com.cy.mediagen.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 租户服务实现类（含乐观锁计费逻辑）
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    /** 乐观锁冲突最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    /** 租户状态 - 正常 */
    private static final int TENANT_STATUS_NORMAL = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long tenantId, int points) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (points <= 0) {
            throw new IllegalArgumentException("扣减点数必须大于0");
        }

        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            // 查询最新的租户信息（包含 version）
            Tenant tenant = getById(tenantId);
            if (tenant == null) {
                throw new IllegalArgumentException("租户不存在，tenantId: " + tenantId);
            }
            if (tenant.getStatus() != TENANT_STATUS_NORMAL) {
                throw new InsufficientBalanceException("租户已被禁用，无法使用算力，tenantId: " + tenantId);
            }
            if (tenant.getBalancePoints() < points) {
                log.warn("租户算力余额不足，tenantId: {}，当前余额: {}，需扣减: {}",
                        tenantId, tenant.getBalancePoints(), points);
                throw new InsufficientBalanceException(
                        "算力余额不足，当前余额: " + tenant.getBalancePoints() + "，需消耗: " + points);
            }

            // 乐观锁扣减：version 字段会自动 +1
            tenant.setBalancePoints(tenant.getBalancePoints() - points);
            boolean success = updateById(tenant);

            if (success) {
                log.info("算力扣减成功，tenantId: {}，扣减: {}，剩余: {}",
                        tenantId, points, tenant.getBalancePoints());
                return;
            }

            // 乐观锁冲突，记录日志后重试
            log.warn("算力扣减乐观锁冲突（第 {} 次重试），tenantId: {}", retry + 1, tenantId);
        }

        // 重试耗尽
        log.error("算力扣减失败，乐观锁重试 {} 次后仍冲突，tenantId: {}", MAX_RETRY_COUNT, tenantId);
        throw new InsufficientBalanceException("系统繁忙，算力扣减失败，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundBalance(Long tenantId, int points) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (points <= 0) {
            throw new IllegalArgumentException("退还点数必须大于0");
        }

        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            Tenant tenant = getById(tenantId);
            if (tenant == null) {
                log.error("退还算力失败，租户不存在，tenantId: {}", tenantId);
                return;
            }

            // 乐观锁加回算力
            tenant.setBalancePoints(tenant.getBalancePoints() + points);
            boolean success = updateById(tenant);

            if (success) {
                log.info("算力退还成功，tenantId: {}，退还: {}，当前余额: {}",
                        tenantId, points, tenant.getBalancePoints());
                return;
            }

            log.warn("算力退还乐观锁冲突（第 {} 次重试），tenantId: {}", retry + 1, tenantId);
        }

        log.error("算力退还失败，乐观锁重试 {} 次后仍冲突，tenantId: {}，退还点数: {}",
                MAX_RETRY_COUNT, tenantId, points);
    }
}
