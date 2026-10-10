package com.loja.checkout.strategy.clube;

import java.math.BigDecimal;

public class BronzeStrategy implements ClubeMemberStrategy {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean validarBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
