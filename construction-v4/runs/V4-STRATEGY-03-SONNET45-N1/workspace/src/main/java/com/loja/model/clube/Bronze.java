package com.loja.model.clube;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return new BigDecimal("0.00");
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
