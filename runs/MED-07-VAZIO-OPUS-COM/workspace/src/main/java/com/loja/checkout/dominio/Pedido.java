package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(ItemPedido::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
