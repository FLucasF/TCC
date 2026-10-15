package com.loja.strategy.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Prata implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public boolean isFretGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean concedeBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
