package com.loja.checkout.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PixPayment implements PaymentStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05");

    @Override
    public String getCode() {
        return "PIX";
    }

    @Override
    public boolean isInstallmentValid(int installments) {
        return installments == 1;
    }

    @Override
    public boolean isAvailable(BigDecimal orderTotal) {
        return true;
    }

    @Override
    public PaymentResult calculate(BigDecimal orderTotal, int installments) {
        BigDecimal discount = orderTotal.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = orderTotal.subtract(discount);
        return new PaymentResult(totalFinal, totalFinal, discount.negate(), 1);
    }
}
