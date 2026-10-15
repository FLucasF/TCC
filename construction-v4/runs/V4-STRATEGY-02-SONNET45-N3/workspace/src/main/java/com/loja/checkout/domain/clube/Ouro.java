package com.loja.checkout.domain.clube;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Ouro implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, 5.0);
    }

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(Dinheiro.de(500.00)) > 0;
    }
}
