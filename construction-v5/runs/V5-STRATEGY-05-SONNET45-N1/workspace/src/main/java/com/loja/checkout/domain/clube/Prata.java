package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.util.Dinheiro;
import java.math.BigDecimal;

public class Prata implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
    }

    @Override
    public boolean freteSempre​Gratis() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
