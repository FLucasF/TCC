package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente: itens, subtotal e peso. */
public record Pedido(List<Item> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos itens (preco x quantidade), arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.valor(itens.stream()
                .map(Item::totalLinha)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido em kg, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::pesoLinha)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
