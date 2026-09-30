package com.loja.checkout.dto;

import java.math.BigDecimal;

public class ItemDTO {
    public String nome;
    public BigDecimal precoUnitario;
    public int quantidade;
    public BigDecimal pesoKg;

    public ItemDTO() {}

    public ItemDTO(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }
}
