package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemPedido> itens) {

    public BigDecimal subtotalProdutos() {
        return itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
