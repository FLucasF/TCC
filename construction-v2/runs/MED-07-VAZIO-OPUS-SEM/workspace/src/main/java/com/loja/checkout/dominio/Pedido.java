package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente: itens, subtotal e peso. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.totalBruto());
        }
        return Dinheiro.arredondar(soma);
    }

    /** Peso do pedido em kg, sem arredondar. */
    public BigDecimal pesoKg() {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.pesoTotalKg());
        }
        return soma;
    }
}
