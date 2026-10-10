package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** 10% de desconto no valor dos produtos. */
public final class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.percentual(contexto.subtotalProdutos(), PERCENTUAL);
    }
}
