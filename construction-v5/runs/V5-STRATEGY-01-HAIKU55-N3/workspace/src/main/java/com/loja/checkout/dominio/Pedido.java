package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {

    public Pedido {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(i -> i == null || !i.valido())) {
            throw new RecusaPedido(Erro.PEDIDO_INVALIDO);
        }
        itens = List.copyOf(itens);
    }

    public BigDecimal subtotal() {
        return Dinheiro.centavos(itens.stream()
                .map(Item::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
