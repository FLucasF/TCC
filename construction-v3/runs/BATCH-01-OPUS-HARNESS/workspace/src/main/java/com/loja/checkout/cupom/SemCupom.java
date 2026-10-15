package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** O caso "o cliente nao usou cupom": desconto zero, sem tratamento especial. */
public final class SemCupom implements Cupom {

    public static final Cupom INSTANCIA = new SemCupom();

    private SemCupom() {
    }

    @Override
    public String codigo() {
        return "";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.ZERO;
    }
}
