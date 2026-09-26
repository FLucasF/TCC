package com.loja.payment;

import java.math.BigDecimal;
import com.loja.util.MoneyRounder;

public class PixForm implements PaymentForm {
    @Override
    public String getCode() {
        return "PIX";
    }

    @Override
    public BigDecimal calculateAdjustment(BigDecimal orderTotal, int installments) {
        BigDecimal discount = MoneyRounder.round(orderTotal.multiply(new BigDecimal("0.05")));
        return discount.negate();
    }

    @Override
    public boolean isAvailable(BigDecimal orderTotal, int installments) {
        return installments == 1;
    }

    @Override
    public int getMaxInstallments() {
        return 1;
    }
}
