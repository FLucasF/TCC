package com.loja.checkout.estrategia.clube;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Prata implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(BigDecimal valorProdutos) {
        return Moeda.arredondar(valorProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal valorProdutos) {
        return false;
    }
}
