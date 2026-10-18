package com.loja.strategy;

import com.loja.util.Money;

public class PixPayment implements PaymentStrategy {
    @Override
    public Double calculateAdjustment(Double subtotal, Integer installments) {
        return Money.round(-subtotal * 0.05);
    }

    @Override
    public boolean isApplicable(Double totalValue, Integer installments) {
        return installments == 1;
    }

    @Override
    public Integer getMaxInstallments() {
        return 1;
    }
}
