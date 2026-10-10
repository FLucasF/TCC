package com.loja.strategy.clube;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public boolean isFretGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return new BigDecimal("0.00");
    }

    @Override
    public boolean concedeBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
