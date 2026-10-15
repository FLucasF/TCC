package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

/** O caso "o cliente não usou cupom": não abate nada, sem condição. */
public final class SemCupom implements Cupom {

    public static final Cupom INSTANCIA = new SemCupom();

    private SemCupom() {
    }

    /** Fica fora do catálogo: este caso vem de o site não informar cupom. */
    @Override
    public String codigo() {
        return "";
    }

    @Override
    public BigDecimal valorAbatido(ContextoCupom contexto) {
        return BigDecimal.ZERO;
    }
}
