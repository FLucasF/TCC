package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Os itens escolhidos pelo cliente. */
public record Carrinho(List<ItemPedido> itens) {

    public Carrinho(List<ItemPedido> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(ItemPedido::totalBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
