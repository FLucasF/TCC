package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {
    @Test
    void testStandardRatePlan() {
        RatePlan plan = new StandardRatePlan();

        assertEquals(RatePlanType.STANDARD, plan.getType());
        assertEquals(BigDecimal.ZERO, plan.calculateDiscount(new BigDecimal("100.00")));
        assertEquals(new BigDecimal("100.00"), plan.calculateRefund(new BigDecimal("100.00")));
    }

    @Test
    void testNonRefundableRatePlan() {
        RatePlan plan = new NonRefundableRatePlan();

        assertEquals(RatePlanType.NON_REFUNDABLE, plan.getType());
        assertEquals(new BigDecimal("15.00"), plan.calculateDiscount(new BigDecimal("100.00")));
        assertEquals(BigDecimal.ZERO, plan.calculateRefund(new BigDecimal("100.00")));
    }
}
