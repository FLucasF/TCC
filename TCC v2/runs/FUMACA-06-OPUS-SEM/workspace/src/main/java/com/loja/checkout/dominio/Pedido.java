package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho do cliente, com os totais que servem de base para todos os calculos. */
public record Pedido(List<Item> itens) {

    public Pedido(List<Item> itens) {
        this.itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        BigDecimal soma = itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(soma);
    }

    /** Peso total em kg, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
