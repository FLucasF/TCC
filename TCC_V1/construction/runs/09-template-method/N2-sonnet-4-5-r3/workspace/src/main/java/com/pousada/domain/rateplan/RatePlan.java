package com.pousada.domain.rateplan;

import java.math.BigDecimal;

public interface RatePlan {
    String getName();
    BigDecimal calculateDiscount(BigDecimal subtotal);
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
