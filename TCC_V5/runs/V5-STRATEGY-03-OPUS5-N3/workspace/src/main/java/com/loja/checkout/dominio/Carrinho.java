package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<ItemPedido> itens) {

    public Carrinho(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada em centavos. */
    public BigDecimal subtotal() {
        return Dinheiro.arredonda(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
