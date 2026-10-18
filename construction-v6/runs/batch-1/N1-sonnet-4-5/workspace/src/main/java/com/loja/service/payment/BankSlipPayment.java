package com.loja.service.payment;

import com.loja.domain.Money;
import com.loja.domain.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class BankSlipPayment implements PaymentStrategy {

    private static final Money FEE = new Money("3.49");
    private static final Money MAX_AMOUNT = new Money("1000.00");

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.BANK_SLIP;
    }

    @Override
    public boolean isAvailable(PaymentContext context) {
        return !context.orderTotal().isGreaterThan(MAX_AMOUNT);
    }

    @Override
    public boolean isValidInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public PaymentResult calculatePayment(PaymentContext context) {
        Money finalTotal = context.orderTotal().add(FEE);
        return new PaymentResult(FEE, finalTotal, finalTotal);
    }
}
