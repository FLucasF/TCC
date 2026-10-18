package com.loja.domain.strategies.payment;

import com.loja.domain.entities.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CreditCardPayment implements PaymentMethodStrategy {
    private static final double MONTHLY_INTEREST_RATE = 0.0199;
    private static final int MAX_INSTALLMENTS_NO_INTEREST = 3;
    private static final int MAX_INSTALLMENTS = 12;

    @Override
    public Money calculateAdjustment(Money orderTotal, int installments, boolean hasCoupon) {
        if (installments <= MAX_INSTALLMENTS_NO_INTEREST) {
            return Money.zero();
        }

        Money installmentAmount = calculateInstallmentWithInterest(orderTotal, installments);
        Money finalTotal = installmentAmount.multiply(installments);
        return finalTotal.subtract(orderTotal);
    }

    @Override
    public int calculateInstallments(int requestedInstallments) {
        if (requestedInstallments < 1) {
            return 1;
        }
        if (requestedInstallments > MAX_INSTALLMENTS) {
            return MAX_INSTALLMENTS;
        }
        return requestedInstallments;
    }

    @Override
    public Money calculateInstallmentAmount(Money finalTotal, int installments) {
        return finalTotal.divide(installments);
    }

    private Money calculateInstallmentWithInterest(Money principal, int installments) {
        BigDecimal rate = BigDecimal.valueOf(MONTHLY_INTEREST_RATE);
        BigDecimal numInstallments = BigDecimal.valueOf(installments);

        BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                onePlusRate.pow(-installments, new java.math.MathContext(10, RoundingMode.HALF_EVEN))
        );

        BigDecimal installment = principal.toBigDecimal()
                .multiply(rate)
                .divide(denominator, 2, RoundingMode.HALF_EVEN);

        return Money.of(installment);
    }


    @Override
    public boolean isAvailable(Money orderTotal) {
        return true;
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CREDIT_CARD;
    }
}
