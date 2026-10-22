package com.pousadamaralta.domain.rateplan;

import java.math.BigDecimal;

public interface RatePlan {
    BigDecimal calculateDiscount(BigDecimal subtotal);
    BigDecimal calculateRefund(BigDecimal amountPaid, BigDecimal total);
}
