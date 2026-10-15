package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** O caso de o cliente não usar cupom: nada a descontar. */
public final class SemCupom implements Cupom {

    public static final Cupom INSTANCIA = new SemCupom();

    private SemCupom() {
    }

    @Override
    public String codigo() {
        return null;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.ZERO;
    }
}
