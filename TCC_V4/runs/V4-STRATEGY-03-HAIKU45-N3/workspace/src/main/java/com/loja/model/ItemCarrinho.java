package com.loja.model;

import java.math.BigDecimal;

public class ItemCarrinho {
    private final String nome;
    private final BigDecimal precoUnitario;
    private final int quantidade;
    private final BigDecimal pesoKg;

    public ItemCarrinho(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
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

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(new BigDecimal(quantidade));
    }

    public BigDecimal getPesoTotal() {
        return pesoKg.multiply(new BigDecimal(quantidade));
    }
}
