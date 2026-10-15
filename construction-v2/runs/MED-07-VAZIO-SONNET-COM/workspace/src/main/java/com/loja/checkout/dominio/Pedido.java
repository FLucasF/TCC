package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {

    public boolean valido() {
        return itens != null && !itens.isEmpty() && itens.stream().allMatch(Item::valido);
    }

    public BigDecimal pesoTotal() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal subtotalProdutos() {
        BigDecimal soma = itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(soma);
    }
}
