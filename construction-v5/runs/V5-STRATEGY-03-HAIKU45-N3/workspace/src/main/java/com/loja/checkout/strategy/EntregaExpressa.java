package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class EntregaExpressa implements EntregaStrategy {

    @Override
    public BigDecimal calcularFrete(Double pesoTotal) {
        return new BigDecimal("25.00")
            .add(new BigDecimal("4.50").multiply(new BigDecimal(pesoTotal)));
    }

    @Override
    public Integer getPrazo() {
        return 2;
    }

    @Override
    public boolean isDisponivel(Double pesoTotal) {
        return true;
    }
}
