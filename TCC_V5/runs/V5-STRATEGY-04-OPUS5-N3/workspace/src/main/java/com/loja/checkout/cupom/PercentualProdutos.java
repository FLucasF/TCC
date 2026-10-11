package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Desconta uma porcentagem do valor dos produtos. */
record PercentualProdutos(String codigo, BigDecimal taxa) implements Cupom {

    PercentualProdutos(String codigo, String taxa) {
        this(codigo, new BigDecimal(taxa));
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), taxa);
    }
}
