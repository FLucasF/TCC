package com.loja.service.payment;

import java.math.BigDecimal;

public interface PaymentAdjuster {
    void validate(Integer parcelas, BigDecimal total);
    PaymentAdjustment calculate(BigDecimal total, Integer parcelas);
}
