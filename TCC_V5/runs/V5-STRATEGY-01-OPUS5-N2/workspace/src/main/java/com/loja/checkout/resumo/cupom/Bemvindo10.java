package com.loja.checkout.resumo.cupom;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** 10% de desconto no valor dos produtos. */
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public BigDecimal desconto(BaseCupom base) {
        return Dinheiro.centavos(base.subtotalProdutos().multiply(PERCENTUAL));
    }
}
