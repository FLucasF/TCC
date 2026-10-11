package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente: o que define o subtotal e o peso do pedido. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredonda(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondamento. */
    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
