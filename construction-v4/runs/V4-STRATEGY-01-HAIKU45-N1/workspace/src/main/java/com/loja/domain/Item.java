package com.loja.domain;

import java.math.BigDecimal;

public class Item {
    public String nome;
    public BigDecimal precoUnitario;
    public Integer quantidade;
    public BigDecimal pesoKg;

    public Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(new BigDecimal(quantidade));
    }
}
