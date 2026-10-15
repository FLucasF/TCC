package com.loja.service.payment;

import com.loja.exception.CheckoutException;
import com.loja.util.MoneyRounder;
import java.math.BigDecimal;

public class CartaoPaymentAdjuster implements PaymentAdjuster {
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");

    @Override
    public void validate(Integer parcelas, BigDecimal total) {
        if (parcelas == null || parcelas < 1 || parcelas > 12) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public PaymentAdjustment calculate(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = total.divide(new BigDecimal(parcelas), 2, java.math.RoundingMode.HALF_EVEN);
            valorParcela = MoneyRounder.round(valorParcela);
            BigDecimal finalTotal = valorParcela.multiply(new BigDecimal(parcelas));
            finalTotal = MoneyRounder.round(finalTotal);
            BigDecimal ajuste = finalTotal.subtract(total);
            ajuste = MoneyRounder.round(ajuste);
            return new PaymentAdjustment(ajuste, parcelas, valorParcela);
        } else {
            BigDecimal rate = MONTHLY_RATE;
            BigDecimal ratePlusBasis = BigDecimal.ONE.add(rate);
            BigDecimal powerTerm = ratePlusBasis.pow(parcelas);
            BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(powerTerm, 10, java.math.RoundingMode.HALF_EVEN)
            );
            BigDecimal valorParcela = total.multiply(rate).divide(denominator, 2, java.math.RoundingMode.HALF_EVEN);
            valorParcela = MoneyRounder.round(valorParcela);
            BigDecimal finalTotal = valorParcela.multiply(new BigDecimal(parcelas));
            finalTotal = MoneyRounder.round(finalTotal);
            BigDecimal ajuste = finalTotal.subtract(total);
            ajuste = MoneyRounder.round(ajuste);
            return new PaymentAdjustment(ajuste, parcelas, valorParcela);
        }
    }
}
