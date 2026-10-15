package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Os produtos do carrinho, já conferidos. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
