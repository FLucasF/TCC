package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente: os itens e o que se deriva deles. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotal() {
        return Dinheiro.centavos(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
