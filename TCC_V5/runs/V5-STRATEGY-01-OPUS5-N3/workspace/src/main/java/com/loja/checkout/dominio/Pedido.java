package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho validado: é a mesma base para frete, cupom, seguro e crédito. */
public record Pedido(List<Item> itens) {

    public Pedido(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return itens.stream()
                .map(Item::total)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    /** Peso do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
