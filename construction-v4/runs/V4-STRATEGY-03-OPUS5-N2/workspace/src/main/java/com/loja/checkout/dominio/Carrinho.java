package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho fechado: itens, soma dos produtos e peso do pedido. */
public record Carrinho(List<Item> itens) {

    public Carrinho(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(somar(Item::valorTotal));
    }

    /** O peso do pedido nao e arredondado. */
    public BigDecimal pesoKg() {
        return somar(Item::pesoTotalKg);
    }

    private BigDecimal somar(java.util.function.Function<Item, BigDecimal> parcela) {
        return itens.stream()
                .map(parcela)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
