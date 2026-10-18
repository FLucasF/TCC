package com.loja.domain.strategies.payment;

import com.loja.domain.entities.*;

public class PixPayment implements PaymentMethodStrategy {

    @Override
    public Money calculateAdjustment(Money orderTotal, int installments, boolean hasCoupon) {
        if (hasCoupon) {
            return Money.zero();
        }
        return orderTotal.multiply(-0.05);
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
        return true;
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.PIX;
    }
}
