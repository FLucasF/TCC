package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** O cliente nao usou cupom: nada a descontar. */
public final class SemCupom implements Cupom {

    public static final Cupom INSTANCIA = new SemCupom();

    private SemCupom() {
    }

    @Override
    public String codigo() {
        return "";
    }

    @Override
    public BigDecimal desconto(ContextoCupom pedido) {
        return Dinheiro.ZERO;
    }
}
