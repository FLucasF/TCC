package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Os itens do carrinho, com as contas que dependem so deles. */
public record Pedido(List<Item> itens) {

    public BigDecimal subtotal() {
        return Dinheiro.centavos(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
