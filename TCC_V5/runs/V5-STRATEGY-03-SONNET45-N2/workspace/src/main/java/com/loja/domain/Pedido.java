package com.loja.domain;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {

    public BigDecimal subtotalProdutos() {
        return itens.stream()
            .map(Item::valorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return itens.stream()
            .map(Item::pesoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
