package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.util.Dinheiro;
import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
    }

    @Override
    public boolean freteSempre​Gratis() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
