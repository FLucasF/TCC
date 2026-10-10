package com.loja.checkout.domain.cupom;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class BemVindo10 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.percentual(contexto.subtotalProdutos(), 10.0);
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
