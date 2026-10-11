package com.loja.checkout.clube;

import com.loja.checkout.calculo.Dinheiro;

import java.math.BigDecimal;

public class Prata implements NivelClube {
    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal frete(BigDecimal freteBase) {
        return Dinheiro.cents(freteBase);
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotal) {
        return Dinheiro.cents(subtotal.multiply(CREDITO));
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return false;
    }
}
