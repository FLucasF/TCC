package com.loja.checkout.clube;

import java.math.BigDecimal;

public class NivelBronze implements NivelClube {

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public boolean ganharBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
