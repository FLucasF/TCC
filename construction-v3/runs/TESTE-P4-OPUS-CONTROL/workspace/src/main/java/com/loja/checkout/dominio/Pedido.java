package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho do cliente com os totais derivados dos itens. */
public record Pedido(List<Item> itens) {

    public Pedido(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos itens, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
