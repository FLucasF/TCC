package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho montado pelo cliente. */
public record Pedido(List<Item> itens) {

    public Pedido(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos: preco de cada item vezes a quantidade. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(soma(Item::valorTotal));
    }

    /** Peso do pedido: peso de cada item vezes a quantidade, sem arredondar. */
    public BigDecimal pesoKg() {
        return soma(Item::pesoTotalKg);
    }

    private BigDecimal soma(java.util.function.Function<Item, BigDecimal> parcela) {
        return itens.stream().map(parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
