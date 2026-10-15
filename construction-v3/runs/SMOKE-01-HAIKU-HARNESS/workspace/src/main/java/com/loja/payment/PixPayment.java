package com.loja.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PixPayment implements PaymentMethod {
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.05");

    @Override
    public boolean isValid(Integer parcelas, BigDecimal total) {
        return parcelas != null && parcelas == 1;
    }

    @Override
    public BigDecimal calculateAdjustment(BigDecimal total, Integer parcelas) {
        BigDecimal discount = total.multiply(DISCOUNT_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_EVEN);
        return discount.negate();
    }

    @Override
    public BigDecimal calculateInstallmentValue(BigDecimal total, Integer parcelas) {
        BigDecimal discount = total.multiply(DISCOUNT_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal finalTotal = total.subtract(discount);
        return finalTotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String getCode() {
        return "PIX";
    }
}
