package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Os produtos escolhidos pelo cliente. */
public record Carrinho(List<ItemPedido> itens) {

    public Carrinho(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada em centavos. */
    public BigDecimal subtotalProdutos() {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.total());
        }
        return Dinheiro.arredondar(soma);
    }

    /** Peso total do pedido em kg, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.peso());
        }
        return soma;
    }
}
