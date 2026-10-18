package com.loja.service.payment;

import com.loja.domain.Money;
import com.loja.domain.PaymentMethod;

public interface PaymentStrategy {
    PaymentMethod getMethod();

    boolean isAvailable(PaymentContext context);

    boolean isValidInstallments(int installments);

    PaymentResult calculatePayment(PaymentContext context);

    record PaymentContext(Money orderTotal, int installments, boolean hasCoupon) {}

    record PaymentResult(Money adjustment, Money finalTotal, Money installmentAmount) {}
}
