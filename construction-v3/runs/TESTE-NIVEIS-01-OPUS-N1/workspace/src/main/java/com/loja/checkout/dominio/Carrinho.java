package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho fechado: o que o pedido tem de produtos, em valor e em peso. */
public record Carrinho(List<Item> itens) {

    public Carrinho(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
