package com.loja.checkout.estrategia.clube;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal valorProdutos) {
        return Moeda.de(0.00);
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
