package com.loja.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Prata implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos
            .multiply(new BigDecimal("0.02"))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean ganhouBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
