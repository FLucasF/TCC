package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResumoResposta {
    @JsonProperty("subtotalProdutos")
    private Double subtotalProdutos;

    @JsonProperty("descontoCupom")
    private Double descontoCupom;

    private Double frete;

    @JsonProperty("prazoEntregaDias")
    private Integer prazoEntregaDias;

    @JsonProperty("ajustePagamento")
    private Double ajustePagamento;

    @JsonProperty("totalFinal")
    private Double totalFinal;

    private Integer parcelas;

    @JsonProperty("valorParcela")
    private Double valorParcela;

    public ResumoResposta(Double subtotalProdutos, Double descontoCupom, Double frete,
                          Integer prazoEntregaDias, Double ajustePagamento, Double totalFinal,
                          Integer parcelas, Double valorParcela) {
        this.subtotalProdutos = subtotalProdutos;
        this.descontoCupom = descontoCupom;
        this.frete = frete;
        this.prazoEntregaDias = prazoEntregaDias;
        this.ajustePagamento = ajustePagamento;
        this.totalFinal = totalFinal;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
    }

    public Double getSubtotalProdutos() {
        return subtotalProdutos;
    }

    public Double getDescontoCupom() {
        return descontoCupom;
    }

    public Double getFrete() {
        return frete;
    }

    public Integer getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public Double getAjustePagamento() {
        return ajustePagamento;
    }

    public Double getTotalFinal() {
        return totalFinal;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public Double getValorParcela() {
        return valorParcela;
    }
}
