package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public boolean zeraFrete() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
