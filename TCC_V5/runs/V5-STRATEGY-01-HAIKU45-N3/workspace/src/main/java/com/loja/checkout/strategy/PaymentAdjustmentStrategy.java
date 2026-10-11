package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface PaymentAdjustmentStrategy {
    void validarParcelas(Integer parcelas);
    BigDecimal calcularAjuste(BigDecimal total, Integer parcelas);
    BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas);
}
