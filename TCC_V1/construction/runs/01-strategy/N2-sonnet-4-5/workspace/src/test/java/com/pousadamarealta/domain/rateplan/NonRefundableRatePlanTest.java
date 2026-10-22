package com.pousadamarealta.domain.rateplan;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class NonRefundableRatePlanTest {

    @Test
    void shouldApply15PercentDiscount() {
        NonRefundableRatePlan ratePlan = new NonRefundableRatePlan();
        BigDecimal subtotal = new BigDecimal("499.80");

        BigDecimal discount = ratePlan.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("74.97"), discount);
    }

    @Test
    void shouldNotRefundAnything() {
        NonRefundableRatePlan ratePlan = new NonRefundableRatePlan();
        BigDecimal amountPaid = new BigDecimal("424.83");

        BigDecimal refund = ratePlan.calculateRefund(amountPaid);

        assertEquals(BigDecimal.ZERO, refund);
    }
}
