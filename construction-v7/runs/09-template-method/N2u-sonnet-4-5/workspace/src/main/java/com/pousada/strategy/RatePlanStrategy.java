package com.pousada.strategy;

import java.math.BigDecimal;

public interface RatePlanStrategy {
    BigDecimal calculateDiscount(BigDecimal subtotal);
    boolean isRefundable();
}
