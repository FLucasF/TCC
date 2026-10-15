package com.loja.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CartaoPayment implements PaymentMethod {
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");
    private static final Integer MAX_INSTALLMENTS = 12;
    private static final Integer FREE_INTEREST_INSTALLMENTS = 3;

    @Override
    public boolean isValid(Integer parcelas, BigDecimal total) {
        return parcelas != null && parcelas >= 1 && parcelas <= MAX_INSTALLMENTS;
    }

    @Override
    public BigDecimal calculateAdjustment(BigDecimal total, Integer parcelas) {
        if (parcelas <= FREE_INTEREST_INSTALLMENTS) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal finalValue = calculateFinalValue(total, parcelas);
        return finalValue.subtract(total).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateInstallmentValue(BigDecimal total, Integer parcelas) {
        if (parcelas <= FREE_INTEREST_INSTALLMENTS) {
            return total.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        BigDecimal finalValue = calculateFinalValue(total, parcelas);
        return finalValue.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateFinalValue(BigDecimal total, Integer parcelas) {
        // parcela = total × taxa ÷ (1 − (1 + taxa)^−número de parcelas)
        // Rewritten as: parcela = total × taxa × (1 + taxa)^n ÷ ((1 + taxa)^n − 1)
        // finalValue = parcela × número de parcelas

        BigDecimal n = new BigDecimal(parcelas);
        BigDecimal ratePlusOne = MONTHLY_RATE.add(BigDecimal.ONE);
        BigDecimal powerValue = ratePlusOne.pow(parcelas);

        BigDecimal numerator = total.multiply(MONTHLY_RATE).multiply(powerValue);
        BigDecimal denominator = powerValue.subtract(BigDecimal.ONE);
        BigDecimal installment = numerator.divide(denominator, 10, RoundingMode.HALF_EVEN);

        return installment.multiply(n).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String getCode() {
        return "CARTAO";
    }
}
