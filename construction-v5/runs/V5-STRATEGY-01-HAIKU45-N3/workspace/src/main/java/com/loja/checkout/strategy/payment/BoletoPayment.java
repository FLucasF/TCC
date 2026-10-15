package com.loja.checkout.strategy.payment;

import com.loja.checkout.strategy.PaymentAdjustmentStrategy;
import com.loja.checkout.util.MoneyRounder;
import java.math.BigDecimal;

public class BoletoPayment implements PaymentAdjustmentStrategy {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

    @Override
    public void validarParcelas(Integer parcelas) {
        // Validação centralizada em CheckoutValidator
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        return MoneyRounder.round(TARIFA);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        BigDecimal valorTotal = total.add(TARIFA);
        return MoneyRounder.round(valorTotal);
    }
}
