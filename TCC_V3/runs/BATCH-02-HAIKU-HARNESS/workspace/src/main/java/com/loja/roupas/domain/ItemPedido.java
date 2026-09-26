package com.loja.roupas.domain;

import java.math.BigDecimal;

public class ItemPedido {
    private final String nome;
    private final BigDecimal precoUnitario;
    private final Integer quantidade;
    private final Double pesoKg;

    public ItemPedido(String nome, BigDecimal precoUnitario, Integer quantidade, Double pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(new BigDecimal(quantidade));
    }

    public Double calcularPesoTotal() {
        return pesoKg * quantidade;
    }
}
