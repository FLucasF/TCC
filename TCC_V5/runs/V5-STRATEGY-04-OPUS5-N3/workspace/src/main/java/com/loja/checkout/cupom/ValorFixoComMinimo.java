package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Desconta um valor fixo nos produtos, a partir de um mínimo em produtos. */
record ValorFixoComMinimo(String codigo, BigDecimal valor, BigDecimal minimoProdutos) implements Cupom {

    ValorFixoComMinimo(String codigo, String valor, String minimoProdutos) {
        this(codigo, new BigDecimal(valor), new BigDecimal(minimoProdutos));
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(minimoProdutos) >= 0;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.centavos(valor);
    }
}
