package com.loja.checkout.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RespostaResumo {
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

    public RespostaResumo() {}

    public RespostaResumo(double subtotalProdutos, double descontoCupom, double frete,
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

    public void setSubtotalProdutos(double subtotalProdutos) {
        this.subtotalProdutos = subtotalProdutos;
    }

    public double getDescontoCupom() {
        return descontoCupom;
    }

    public void setDescontoCupom(double descontoCupom) {
        this.descontoCupom = descontoCupom;
    }

    public double getFrete() {
        return frete;
    }

    public void setFrete(double frete) {
        this.frete = frete;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public void setPrazoEntregaDias(int prazoEntregaDias) {
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public double getAjustePagamento() {
        return ajustePagamento;
    }

    public void setAjustePagamento(double ajustePagamento) {
        this.ajustePagamento = ajustePagamento;
    }

    public double getTotalFinal() {
        return totalFinal;
    }

    public void setTotalFinal(double totalFinal) {
        this.totalFinal = totalFinal;
    }

    public int getParcelas() {
        return parcelas;
    }

    public void setParcelas(int parcelas) {
        this.parcelas = parcelas;
    }

    public double getValorParcela() {
        return valorParcela;
    }

    public void setValorParcela(double valorParcela) {
        this.valorParcela = valorParcela;
    }
}
