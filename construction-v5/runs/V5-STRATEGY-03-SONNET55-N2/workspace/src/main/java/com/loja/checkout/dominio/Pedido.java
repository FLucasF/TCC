package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {

    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(Item::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
