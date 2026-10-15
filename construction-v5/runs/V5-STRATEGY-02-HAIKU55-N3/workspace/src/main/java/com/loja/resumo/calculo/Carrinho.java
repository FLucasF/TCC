package com.loja.resumo.calculo;

import java.math.BigDecimal;
import java.util.List;

record Carrinho(List<Item> itens) {

    boolean valido() {
        return itens != null && !itens.isEmpty()
                && itens.stream().allMatch(item -> item != null && item.valido());
    }

    BigDecimal subtotal() {
        return itens.stream().map(Item::subtotal).reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    BigDecimal peso() {
        return itens.stream().map(Item::peso).reduce(Dinheiro.ZERO, BigDecimal::add);
    }
}
