package com.loja.strategy;

import com.loja.util.Money;

public class BankSlipPayment implements PaymentStrategy {
    @Override
    public Double calculateAdjustment(Double subtotal, Integer installments) {
        return Money.round(3.49);
    }

    @Override
    public boolean isApplicable(Double totalValue, Integer installments) {
        return installments == 1 && totalValue <= 1000.0;
    }

    @Override
    public Integer getMaxInstallments() {
        return 1;
    }
}
