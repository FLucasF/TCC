package com.loja.payment;

import java.math.BigDecimal;

public interface PaymentForm {
    String getCode();
    BigDecimal calculateAdjustment(BigDecimal orderTotal, int installments);
    boolean isAvailable(BigDecimal orderTotal, int installments);
    int getMaxInstallments();
}
