package com.loja.service.payment;

import com.loja.domain.Money;
import com.loja.domain.PaymentMethod;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CreditCardPayment implements PaymentStrategy {

    private static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.0199");

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CREDIT_CARD;
    }

    @Override
    public boolean isAvailable(PaymentContext context) {
        return true;
    }

    @Override
    public boolean isValidInstallments(int installments) {
        return installments >= 1 && installments <= 12;
    }

    @Override
    public PaymentResult calculatePayment(PaymentContext context) {
        if (context.installments() <= 3) {
            Money installmentAmount = context.orderTotal().divide(context.installments());
            return new PaymentResult(Money.zero(), context.orderTotal(), installmentAmount);
        }

        BigDecimal rate = MONTHLY_INTEREST_RATE;
        int n = context.installments();

        BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
        BigDecimal powerFactor = onePlusRate.pow(n);

        BigDecimal denominator = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(powerFactor, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal installmentValue = context.orderTotal().value()
            .multiply(rate)
            .divide(denominator, 2, RoundingMode.HALF_EVEN);

        Money installmentAmount = new Money(installmentValue);
        Money finalTotal = installmentAmount.multiply(context.installments());
        Money adjustment = finalTotal.subtract(context.orderTotal());

        return new PaymentResult(adjustment, finalTotal, installmentAmount);
    }
}
