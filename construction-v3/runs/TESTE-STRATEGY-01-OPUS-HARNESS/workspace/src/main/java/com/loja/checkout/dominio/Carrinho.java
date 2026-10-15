package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho ja validado: sabe somar produtos e peso. */
public record Carrinho(List<Item> itens) {

    public Carrinho(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

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
