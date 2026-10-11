package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {

    public Carrinho(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(Item::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido: soma dos pesos dos itens, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
