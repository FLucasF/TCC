package com.loja.domain.clube;

import com.loja.util.Dinheiro;
import java.math.BigDecimal;

public class Prata implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
    }

    @Override
    public boolean isFreteCortesia() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
