package com.loja.resumo.dominio;

import com.loja.resumo.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {

    public BigDecimal subtotalProdutos() {
        BigDecimal soma = itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(soma);
    }

    public BigDecimal pesoTotal() {
        return itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
