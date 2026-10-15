package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho já validado: os itens e o que se calcula a partir deles. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(ItemPedido::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
