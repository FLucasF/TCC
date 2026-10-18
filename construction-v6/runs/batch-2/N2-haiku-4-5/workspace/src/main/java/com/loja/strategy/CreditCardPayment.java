package com.loja.strategy;

import com.loja.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CreditCardPayment implements PaymentStrategy {
    private static final double MONTHLY_RATE = 0.0199;

    @Override
    public Double calculateAdjustment(Double subtotal, Integer installments) {
        if (installments <= 3) {
            return 0.0;
        }

        double monthlyRate = MONTHLY_RATE;
        double numerator = monthlyRate;
        double denominator = 1.0 - Math.pow(1.0 + monthlyRate, -installments);
        double installmentValue = subtotal * (numerator / denominator);

        Double totalWithInterest = Money.round(installmentValue * installments);
        Double interestAmount = totalWithInterest - subtotal;

        return Money.round(interestAmount);
    }

    @Override
    public boolean isApplicable(Double totalValue, Integer installments) {
        return installments >= 1 && installments <= 12;
    }

    @Override
    public Integer getMaxInstallments() {
        return 12;
    }
}
