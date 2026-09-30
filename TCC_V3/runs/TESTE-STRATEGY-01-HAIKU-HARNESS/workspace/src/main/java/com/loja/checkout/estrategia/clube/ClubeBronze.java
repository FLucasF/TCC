package com.loja.checkout.estrategia.clube;

import java.math.BigDecimal;

public class ClubeBronze implements EstrategiaClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getDescontoFrete() {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean verificarBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
