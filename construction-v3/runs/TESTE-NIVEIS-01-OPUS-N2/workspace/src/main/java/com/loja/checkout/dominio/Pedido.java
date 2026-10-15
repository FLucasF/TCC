package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho fechado: itens, valor dos produtos e peso total. */
public final class Pedido {

    private final List<Item> itens;
    private final BigDecimal subtotalProdutos;
    private final BigDecimal pesoKg;

    public Pedido(List<Item> itens) {
        this.itens = List.copyOf(itens);
        this.subtotalProdutos = Dinheiro.centavos(this.itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        this.pesoKg = this.itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Item> itens() {
        return itens;
    }

    public BigDecimal subtotalProdutos() {
        return subtotalProdutos;
    }

    /** Peso do pedido em quilos, sem arredondamento. */
    public BigDecimal pesoKg() {
        return pesoKg;
    }
}
