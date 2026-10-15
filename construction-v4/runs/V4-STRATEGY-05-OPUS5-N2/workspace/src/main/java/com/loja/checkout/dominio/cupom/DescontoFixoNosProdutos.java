package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Desconta um valor fixo dos produtos, a partir de um minimo de compra. */
public record DescontoFixoNosProdutos(BigDecimal valor, BigDecimal minimoProdutos) implements Cupom {

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.emCentavos(valor);
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.subtotalProdutos().compareTo(minimoProdutos) >= 0;
    }
}
