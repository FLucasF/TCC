package com.loja.checkout.domain;

import java.math.BigDecimal;

public class Item {
    private final String nome;
    private final BigDecimal precoUnitario;
    private final int quantidade;
    private final BigDecimal pesoKg;

    public Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
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

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public BigDecimal getPrecoTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal getPesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
