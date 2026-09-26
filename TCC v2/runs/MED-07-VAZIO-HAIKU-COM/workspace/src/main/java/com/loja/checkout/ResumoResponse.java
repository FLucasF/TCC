package com.loja.checkout;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResumoResponse {
    @JsonProperty("subtotalProdutos")
    private double subtotalProdutos;

    @JsonProperty("descontoCupom")
    private double descontoCupom;

    @JsonProperty("frete")
    private double frete;

    @JsonProperty("prazoEntregaDias")
    private int prazoEntregaDias;

    @JsonProperty("ajustePagamento")
    private double ajustePagamento;

    @JsonProperty("totalFinal")
    private double totalFinal;

    @JsonProperty("parcelas")
    private int parcelas;

    @JsonProperty("valorParcela")
    private double valorParcela;

    public ResumoResponse(double subtotalProdutos, double descontoCupom, double frete,
                         int prazoEntregaDias, double ajustePagamento, double totalFinal,
                         int parcelas, double valorParcela) {
        this.subtotalProdutos = subtotalProdutos;
        this.descontoCupom = descontoCupom;
        this.frete = frete;
        this.prazoEntregaDias = prazoEntregaDias;
        this.ajustePagamento = ajustePagamento;
        this.totalFinal = totalFinal;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
    }

    public double getSubtotalProdutos() {
        return subtotalProdutos;
    }

    public double getDescontoCupom() {
        return descontoCupom;
    }

    public double getFrete() {
        return frete;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public double getAjustePagamento() {
        return ajustePagamento;
    }

    public double getTotalFinal() {
        return totalFinal;
    }

    public int getParcelas() {
        return parcelas;
    }

    public double getValorParcela() {
        return valorParcela;
    }
}
