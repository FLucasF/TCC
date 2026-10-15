package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Preco de cada item vezes a quantidade, arredondado em centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso de cada item vezes a quantidade, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
