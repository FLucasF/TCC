package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho ja validado: o que os calculos de entrega e cupom precisam saber. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos, em centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
