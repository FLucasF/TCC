package com.loja.checkout.domain.clube;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Bronze implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.de(0.00);
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
