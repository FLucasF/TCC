package com.loja.domain.clube;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return new BigDecimal("0.00");
    }

    @Override
    public boolean isFreteCortesia() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
