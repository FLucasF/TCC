package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente, ja validado. */
public record Carrinho(List<Item> itens) {

    public Carrinho(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos: preco de cada item x quantidade, em centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido: peso de cada item x quantidade, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(Item::pesoTotalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
