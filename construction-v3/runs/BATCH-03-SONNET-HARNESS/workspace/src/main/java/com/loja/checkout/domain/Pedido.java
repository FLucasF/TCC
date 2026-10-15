package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemPedido> itens) {

    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotalItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal subtotalProdutos() {
        return itens.stream()
                .map(ItemPedido::totalItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
