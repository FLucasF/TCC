package com.loja.clube;

import java.math.BigDecimal;

public class Bronze implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO.setScale(2);
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
