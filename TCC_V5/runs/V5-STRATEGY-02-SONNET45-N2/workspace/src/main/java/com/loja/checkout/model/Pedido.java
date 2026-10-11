package com.loja.checkout.model;

import com.loja.checkout.enums.Regiao;
import java.math.BigDecimal;
import java.util.List;

public record Pedido(
    List<Item> itens,
    Regiao regiao
) {
    public BigDecimal subtotalProdutos() {
        return itens.stream()
            .map(Item::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return itens.stream()
            .map(Item::pesoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
