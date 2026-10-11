package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens, BigDecimal subtotal, BigDecimal pesoKg) {

    public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
        public BigDecimal total() {
            return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
        public BigDecimal peso() {
            return pesoKg.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    public static Pedido de(List<Item> itens) {
        BigDecimal subtotal = itens.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Pedido(itens, Dinheiro.emCentavos(subtotal), peso);
    }
}
