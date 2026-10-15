package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {
    public BigDecimal subtotal() {
        return Dinheiro.arredondar(itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal pesoKg() {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
