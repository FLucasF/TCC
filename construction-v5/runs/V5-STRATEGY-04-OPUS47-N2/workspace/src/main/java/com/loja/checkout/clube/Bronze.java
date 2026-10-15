package com.loja.checkout.clube;

import com.loja.checkout.calculo.Dinheiro;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public BigDecimal frete(BigDecimal freteBase) {
        return Dinheiro.cents(freteBase);
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotal) {
        return Dinheiro.zero();
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return false;
    }
}
