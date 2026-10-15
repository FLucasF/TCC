package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Ganha 2% do valor dos produtos de volta, em credito para a proxima compra. */
public final class Prata implements Clube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("2");

    @Override
    public Vantagens vantagens(BigDecimal subtotalProdutos, BigDecimal freteCalculado) {
        return new Vantagens(
                Dinheiro.percentual(PERCENTUAL_CREDITO, subtotalProdutos),
                Dinheiro.emCentavos(freteCalculado),
                false);
    }
}
