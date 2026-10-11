package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente: os itens e os números que saem deles. */
public record Pedido(List<Item> itens) {

    public Pedido(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos itens (preço × quantidade), arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(Item::totalLinha)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido em kg, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(Item::pesoTotalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
