package com.loja.checkout.domain.cupom;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Menos50 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.de(50.00);
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.subtotalProdutos().compareTo(Dinheiro.de(300.00)) >= 0;
    }
}
