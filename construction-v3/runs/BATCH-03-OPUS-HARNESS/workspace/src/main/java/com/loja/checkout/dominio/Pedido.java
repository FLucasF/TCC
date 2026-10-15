package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho ja validado. */
public record Pedido(List<Item> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada em centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(Item::totalItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
