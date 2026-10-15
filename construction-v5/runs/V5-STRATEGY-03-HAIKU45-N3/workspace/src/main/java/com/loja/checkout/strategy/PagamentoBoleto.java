package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class PagamentoBoleto implements PagamentoStrategy {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        return TARIFA;
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
        return total.compareTo(LIMITE_MAXIMO) <= 0;
    }
}
