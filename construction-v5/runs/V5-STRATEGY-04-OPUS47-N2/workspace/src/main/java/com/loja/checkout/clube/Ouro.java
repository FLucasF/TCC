package com.loja.checkout.clube;

import com.loja.checkout.calculo.Dinheiro;

import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal frete(BigDecimal freteBase) {
        return Dinheiro.zero();
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotal) {
        return Dinheiro.cents(subtotal.multiply(CREDITO));
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return subtotal.compareTo(LIMITE_BRINDE) > 0;
    }
}
