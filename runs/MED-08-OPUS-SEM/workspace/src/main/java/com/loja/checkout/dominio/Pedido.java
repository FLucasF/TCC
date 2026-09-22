package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho validado, com os numeros que as demais regras usam. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada em centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido em kg, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
