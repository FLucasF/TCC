package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {

    public Pedido(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(soma(Item::total));
    }

    public BigDecimal pesoTotalKg() {
        return soma(Item::peso);
    }

    private BigDecimal soma(java.util.function.Function<Item, BigDecimal> parcela) {
        return itens.stream().map(parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
