package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements EntregaStrategy {

    @Override
    public BigDecimal calcularFrete(Double pesoTotal) {
        return new BigDecimal("0.00");
    }

    @Override
    public Integer getPrazo() {
        return 1;
    }

    @Override
    public boolean isDisponivel(Double pesoTotal) {
        return true;
    }
}
