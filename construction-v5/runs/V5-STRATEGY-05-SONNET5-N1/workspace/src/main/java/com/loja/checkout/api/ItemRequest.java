package com.loja.checkout.api;

import java.math.BigDecimal;

/**
 * Representação crua de um item do carrinho tal como recebido no JSON.
 * Os campos são deliberadamente "nullable" (tipos boxed) para permitir detectar ausência
 * de valor na validação, antes de converter para o domínio.
 */
public class ItemRequest {

    private String nome;
    private BigDecimal precoUnitario;
    private Integer quantidade;
    private BigDecimal pesoKg;

    public ItemRequest() {
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
}
