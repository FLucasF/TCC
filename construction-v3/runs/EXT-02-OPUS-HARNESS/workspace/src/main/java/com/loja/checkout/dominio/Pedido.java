package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemPedido> itens) {

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream().map(ItemPedido::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em quilos, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(ItemPedido::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
