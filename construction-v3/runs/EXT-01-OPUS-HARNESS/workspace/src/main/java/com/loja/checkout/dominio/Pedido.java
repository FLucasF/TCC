package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(soma(ItemPedido::total));
    }

    /** O peso do pedido nao e arredondado: e usado como esta no calculo do frete. */
    public BigDecimal pesoKg() {
        return soma(ItemPedido::peso);
    }

    private BigDecimal soma(java.util.function.Function<ItemPedido, BigDecimal> parcela) {
        return itens.stream().map(parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
