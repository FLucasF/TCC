package com.loja.checkout.strategy.payment;

import com.loja.checkout.strategy.PaymentAdjustmentStrategy;
import com.loja.checkout.util.MoneyRounder;
import java.math.BigDecimal;

public class PixPayment implements PaymentAdjustmentStrategy {
    @Override
    public void validarParcelas(Integer parcelas) {
        // Validação centralizada em CheckoutValidator
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = calcularDesconto(total);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = calcularDesconto(total);
        BigDecimal totalComDesconto = total.subtract(desconto);
        return MoneyRounder.round(totalComDesconto);
    }

    private BigDecimal calcularDesconto(BigDecimal total) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
        return MoneyRounder.round(desconto);
    }
}
