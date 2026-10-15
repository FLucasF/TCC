package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class PagamentoPix implements PagamentoStrategy {

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal valorFinal, Integer parcelas) {
        return valorFinal;
    }

    @Override
    public Integer getParcelasMaximas() {
        return 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal total) {
        return true;
    }
}
