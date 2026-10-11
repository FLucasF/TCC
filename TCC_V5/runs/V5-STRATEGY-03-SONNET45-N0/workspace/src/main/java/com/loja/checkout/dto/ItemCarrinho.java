package com.loja.checkout.dto;

import java.math.BigDecimal;

public class ItemCarrinho {
    private String nome;
    private BigDecimal precoUnitario;
    private Integer quantidade;
    private BigDecimal pesoKg;

    public ItemCarrinho() {
    }

    public ItemCarrinho(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public boolean isValido() {
        return nome != null && !nome.isEmpty() &&
               precoUnitario != null && precoUnitario.compareTo(BigDecimal.ZERO) > 0 &&
               quantidade != null && quantidade > 0 &&
               pesoKg != null && pesoKg.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(new BigDecimal(quantidade));
    }

    public BigDecimal calcularPesoTotal() {
        return pesoKg.multiply(new BigDecimal(quantidade));
    }
}
