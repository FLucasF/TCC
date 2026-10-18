package com.loja.strategy.payment;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public interface PaymentStrategy {
    String getCode();
    PaymentResult calculate(CheckoutRequest request, BigDecimal orderTotal, boolean hasCoupon);
    boolean isAvailable(CheckoutRequest request, BigDecimal orderTotal);
    boolean isValidInstallments(int installments);
}
