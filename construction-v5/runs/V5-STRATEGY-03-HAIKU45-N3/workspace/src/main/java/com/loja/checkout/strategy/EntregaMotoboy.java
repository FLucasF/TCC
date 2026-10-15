package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class EntregaMotoboy implements EntregaStrategy {

    private static final double PESO_MAXIMO = 5.0;

    @Override
    public BigDecimal calcularFrete(Double pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public Integer getPrazo() {
        return 0;
    }

    @Override
    public boolean isDisponivel(Double pesoTotal) {
        return pesoTotal <= PESO_MAXIMO;
    }
}
