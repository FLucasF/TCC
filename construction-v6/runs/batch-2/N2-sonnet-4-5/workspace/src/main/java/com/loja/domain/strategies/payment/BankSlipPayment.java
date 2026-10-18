package com.loja.domain.strategies.payment;

import com.loja.domain.entities.*;

public class BankSlipPayment implements PaymentMethodStrategy {
    private static final Money FEE = Money.of(3.49);
    private static final Money MAX_AMOUNT = Money.of(1000.00);

    @Override
    public Money calculateAdjustment(Money orderTotal, int installments, boolean hasCoupon) {
        return FEE;
    }

    @Override
    public int calculateInstallments(int requestedInstallments) {
        return 1;
    }

    @Override
    public Money calculateInstallmentAmount(Money finalTotal, int installments) {
        return finalTotal;
    }

    @Override
    public boolean isAvailable(Money orderTotal) {
        return !orderTotal.isGreaterThan(MAX_AMOUNT);
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.BANK_SLIP;
    }
}
