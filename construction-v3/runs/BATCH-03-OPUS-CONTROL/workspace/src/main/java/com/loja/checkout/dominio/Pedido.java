package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente: itens, subtotal dos produtos e peso total. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        BigDecimal soma = itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(soma);
    }

    /** Peso do pedido em kg, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
