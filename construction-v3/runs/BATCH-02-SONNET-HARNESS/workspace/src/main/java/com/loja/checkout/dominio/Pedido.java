package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal pesoTotal) {

    public static Pedido de(List<Item> itens) {
        BigDecimal subtotal = itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Pedido(itens, Dinheiro.arredondar(subtotal), peso);
    }
}
