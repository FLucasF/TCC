package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Desconta uma porcentagem do valor dos produtos. */
public record DescontoPercentualNosProdutos(BigDecimal percentual) implements Cupom {

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.percentual(percentual, contexto.subtotalProdutos());
    }
}
