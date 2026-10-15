package com.loja.resumo.calculo;

import java.math.BigDecimal;
import java.util.List;

import static com.loja.resumo.calculo.Dinheiro.ZERO;

record Pedido(List<Item> itens) {

    BigDecimal subtotal() {
        return itens.stream().map(Item::subtotal).reduce(ZERO, BigDecimal::add);
    }

    BigDecimal pesoKg() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
