package com.cy.mediagen.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 算力扣减注解
 * <p>
 * 标记在 Controller 方法上，由 BillingAspect 切面自动执行算力预扣。
 * 若业务异常，自动退还已扣算力，保证最终一致性。
 * </p>
 *
 * @author cy
 * @date 2026-03-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeductPoints {

    /**
     * 需扣除的算力点数，默认 10
     */
    int value() default 10;
}
