package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {
    public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
        public BigDecimal subtotal() {
            return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
        public BigDecimal pesoTotal() {
            return pesoKg.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    public BigDecimal subtotalProdutos() {
        return itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoKg() {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
