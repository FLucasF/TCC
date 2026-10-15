package com.loja.payment;

import java.math.BigDecimal;

public interface PaymentMethod {
    boolean isValid(Integer parcelas, BigDecimal total);
    BigDecimal calculateAdjustment(BigDecimal total, Integer parcelas);
    BigDecimal calculateInstallmentValue(BigDecimal total, Integer parcelas);
    String getCode();
}
