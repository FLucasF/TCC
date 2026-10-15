package com.loja.checkout.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class CartaoPayment implements PaymentStrategy {

    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");
    private static final int MAX_NO_INTEREST = 3;
    private static final int MAX_INSTALLMENTS = 12;

    @Override
    public String getCode() {
        return "CARTAO";
    }

    @Override
    public boolean isInstallmentValid(int installments) {
        return installments >= 1 && installments <= MAX_INSTALLMENTS;
    }

    @Override
    public boolean isAvailable(BigDecimal orderTotal) {
        return true;
    }

    @Override
    public PaymentResult calculate(BigDecimal orderTotal, int installments) {
        if (installments <= MAX_NO_INTEREST) {
            BigDecimal parcela = orderTotal.divide(new BigDecimal(installments), 2, RoundingMode.HALF_EVEN);
            return new PaymentResult(orderTotal, parcela, BigDecimal.ZERO.setScale(2), installments);
        }

        MathContext mc = MathContext.DECIMAL128;
        BigDecimal onePlusRate = BigDecimal.ONE.add(MONTHLY_RATE);
        BigDecimal power = onePlusRate.pow(installments, mc);
        BigDecimal numerator = orderTotal.multiply(MONTHLY_RATE, mc);
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(power, mc), mc);
        BigDecimal parcela = numerator.divide(denominator, 2, RoundingMode.HALF_EVEN);

        BigDecimal totalFinal = parcela.multiply(new BigDecimal(installments));
        BigDecimal ajuste = totalFinal.subtract(orderTotal).setScale(2, RoundingMode.HALF_EVEN);

        return new PaymentResult(totalFinal, parcela, ajuste, installments);
    }
}
