package com.loja.service.payment;

import com.loja.exception.CheckoutException;
import com.loja.util.MoneyRounder;
import java.math.BigDecimal;

public class PixPaymentAdjuster implements PaymentAdjuster {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05");

    @Override
    public void validate(Integer parcelas, BigDecimal total) {
        if (parcelas != null && parcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public PaymentAdjustment calculate(BigDecimal total, Integer parcelas) {
        BigDecimal discount = total.multiply(DISCOUNT_RATE);
        discount = MoneyRounder.round(discount);
        BigDecimal finalTotal = total.subtract(discount);
        finalTotal = MoneyRounder.round(finalTotal);
        BigDecimal ajuste = finalTotal.subtract(total);
        ajuste = MoneyRounder.round(ajuste);
        return new PaymentAdjustment(ajuste, 1, finalTotal);
    }
}
