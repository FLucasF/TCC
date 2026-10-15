package com.loja.checkout.domain.pedido;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho do cliente: a base de tudo que e calculado no resumo. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.total());
        }
        return Dinheiro.valor(soma);
    }

    /** Peso do pedido em kg, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.peso());
        }
        return soma;
    }
}
