package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<ItemPedido> itens) {

    public boolean valido() {
        return itens != null && !itens.isEmpty() && itens.stream().allMatch(ItemPedido::valido);
    }

    public BigDecimal subtotalProdutos() {
        return itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
