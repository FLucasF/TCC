package com.pousadamarealta.domain;

import java.math.BigDecimal;

public interface RatePlan {
    RatePlanType getType();
    BigDecimal calculateDiscount(BigDecimal subtotal);
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
