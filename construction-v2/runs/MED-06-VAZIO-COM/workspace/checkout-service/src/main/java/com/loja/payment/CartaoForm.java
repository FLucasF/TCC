package com.loja.payment;

import java.math.BigDecimal;
import com.loja.util.MoneyRounder;

public class CartaoForm implements PaymentForm {
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");

    @Override
    public String getCode() {
        return "CARTAO";
    }

    @Override
    public BigDecimal calculateAdjustment(BigDecimal orderTotal, int installments) {
        if (installments <= 3) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = MONTHLY_RATE;
        BigDecimal numerator = orderTotal.multiply(rate);

        BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
        BigDecimal onePlusRatePower = onePlusRate.pow(installments);
        BigDecimal denominator = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(onePlusRatePower, 10, java.math.RoundingMode.HALF_EVEN)
        );

        BigDecimal installmentValue = numerator.divide(denominator, 10, java.math.RoundingMode.HALF_EVEN);
        installmentValue = MoneyRounder.round(installmentValue);

        BigDecimal totalWithInterest = installmentValue.multiply(new BigDecimal(installments));
        return MoneyRounder.round(totalWithInterest.subtract(orderTotal));
    }

    @Override
    public boolean isAvailable(BigDecimal orderTotal, int installments) {
        return installments >= 1 && installments <= 12;
    }

    @Override
    public int getMaxInstallments() {
        return 12;
    }
}
