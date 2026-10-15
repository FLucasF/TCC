package com.loja.checkout.domain;

import com.loja.checkout.enums.Regiao;

import java.math.BigDecimal;
import java.util.List;

public class Pedido {
    private final List<Item> itens;
    private final Regiao regiao;

    public Pedido(List<Item> itens, Regiao regiao) {
        this.itens = itens;
        this.regiao = regiao;
    }

    public List<Item> getItens() {
        return itens;
    }

    public Regiao getRegiao() {
        return regiao;
    }

    public BigDecimal getSubtotalProdutos() {
        return itens.stream()
                .map(Item::getPrecoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPesoTotal() {
        return itens.stream()
                .map(Item::getPesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
