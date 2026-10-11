package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;

/** Pedido em que o cliente não usou cupom. Não fica no catálogo: não tem código. */
public final class SemCupom implements Cupom {

    public static final SemCupom INSTANCIA = new SemCupom();

    private SemCupom() {
    }

    @Override
    public String codigo() {
        return "";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.ZERO;
    }
}
