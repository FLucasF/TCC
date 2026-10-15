package com.loja.resumo;

import java.math.BigDecimal;
import java.util.List;

record Compra(List<ItemCompra> itens) {

    BigDecimal subtotal() {
        return Dinheiro.arredondar(itens.stream()
                .map(ItemCompra::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemCompra::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
