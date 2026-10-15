package com.loja.domain;

import java.math.BigDecimal;

public class Item {
    private String nome;
    private BigDecimal precoUnitario;
    private Integer quantidade;
    private BigDecimal pesoKg;

    public Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
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

    public BigDecimal getPesoKg() {
        return pesoKg;
    }
}
