package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class EntregaEconomica implements EntregaStrategy {

    @Override
    public BigDecimal calcularFrete(Double pesoTotal) {
        return new BigDecimal("12.00")
            .add(new BigDecimal("2.00").multiply(new BigDecimal(pesoTotal)));
    }

    @Override
    public Integer getPrazo() {
        return 7;
    }

    @Override
    public boolean isDisponivel(Double pesoTotal) {
        return true;
    }
}
