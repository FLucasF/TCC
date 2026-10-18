package com.loja.domain.strategies.payment;

import com.loja.domain.entities.*;

public interface PaymentMethodStrategy {
    Money calculateAdjustment(Money orderTotal, int installments, boolean hasCoupon);

    int calculateInstallments(int requestedInstallments);

    Money calculateInstallmentAmount(Money finalTotal, int installments);

    boolean isAvailable(Money orderTotal);

    PaymentMethod getMethod();
}
