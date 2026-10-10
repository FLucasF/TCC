package com.loja.checkout.clube;

import java.math.BigDecimal;

public class Bronze implements NivelClube {

    @Override
    public BigDecimal creditoPercentual() {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
