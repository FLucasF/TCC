package com.loja.checkout.dominio;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.util.List;

/** Carrinho do cliente: itens, subtotal e peso. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos (preco unitario x quantidade), arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondamento. */
    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
