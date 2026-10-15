package com.loja.checkout.clube;

import java.math.BigDecimal;

public class BronzeStrategy implements ClubeStrategy {
    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
