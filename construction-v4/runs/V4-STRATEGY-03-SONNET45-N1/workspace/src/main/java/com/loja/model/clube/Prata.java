package com.loja.model.clube;

import com.loja.util.Dinheiro;

import java.math.BigDecimal;

public class Prata implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
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
