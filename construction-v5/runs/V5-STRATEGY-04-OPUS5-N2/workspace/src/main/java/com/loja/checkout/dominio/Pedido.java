package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemPedido> itens) {

    public Pedido(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos: preco de cada item vezes a quantidade. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(soma(ItemPedido::total));
    }

    /** Peso do pedido: soma do peso de cada item vezes a quantidade, sem arredondar. */
    public BigDecimal pesoKg() {
        return soma(ItemPedido::peso);
    }

    private BigDecimal soma(java.util.function.Function<ItemPedido, BigDecimal> parcela) {
        return itens.stream().map(parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
