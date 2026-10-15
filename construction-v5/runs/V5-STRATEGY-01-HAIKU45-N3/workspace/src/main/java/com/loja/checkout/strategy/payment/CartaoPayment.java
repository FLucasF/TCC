package com.loja.checkout.strategy.payment;

import com.loja.checkout.strategy.PaymentAdjustmentStrategy;
import com.loja.checkout.util.MoneyRounder;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CartaoPayment implements PaymentAdjustmentStrategy {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public void validarParcelas(Integer parcelas) {
        // Validação centralizada em CheckoutValidator
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return MoneyRounder.round(BigDecimal.ZERO);
        }

        BigDecimal valorParcela = calcularValorParcela(total, parcelas);
        BigDecimal valorFinal = valorParcela.multiply(new BigDecimal(parcelas));
        BigDecimal ajuste = valorFinal.subtract(total);
        return MoneyRounder.round(ajuste);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return MoneyRounder.round(total.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal expoente = umMaisTaxa.pow(-parcelas, new java.math.MathContext(10));
        BigDecimal denominador = BigDecimal.ONE.subtract(expoente);
        BigDecimal parcela = total.multiply(TAXA_JUROS).divide(denominador, 10, RoundingMode.HALF_EVEN);
        return MoneyRounder.round(parcela);
    }
}
