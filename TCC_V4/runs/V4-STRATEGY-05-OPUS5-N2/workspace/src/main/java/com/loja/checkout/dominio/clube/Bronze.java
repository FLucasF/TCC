package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** So o cadastro: nao ganha nada. */
public final class Bronze implements Clube {

    @Override
    public Vantagens vantagens(BigDecimal subtotalProdutos, BigDecimal freteCalculado) {
        return new Vantagens(Dinheiro.ZERO, Dinheiro.emCentavos(freteCalculado), false);
    }
}
