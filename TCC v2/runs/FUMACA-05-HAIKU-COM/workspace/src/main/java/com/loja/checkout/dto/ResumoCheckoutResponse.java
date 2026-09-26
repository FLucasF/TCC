package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResumoCheckoutResponse {

    @JsonProperty("subtotalProdutos")
    private Double subtotalProdutos;

    @JsonProperty("descontoCupom")
    private Double descontoCupom;

    @JsonProperty("frete")
    private Double frete;

    @JsonProperty("prazoEntregaDias")
    private Integer prazoEntregaDias;

    @JsonProperty("ajustePagamento")
    private Double ajustePagamento;

    @JsonProperty("totalFinal")
    private Double totalFinal;

    @JsonProperty("parcelas")
    private Integer parcelas;

    @JsonProperty("valorParcela")
    private Double valorParcela;

    public ResumoCheckoutResponse() {
    }

    public ResumoCheckoutResponse(Double subtotalProdutos, Double descontoCupom, Double frete,
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

    public void setSubtotalProdutos(Double subtotalProdutos) {
        this.subtotalProdutos = subtotalProdutos;
    }

    public Double getDescontoCupom() {
        return descontoCupom;
    }

    public void setDescontoCupom(Double descontoCupom) {
        this.descontoCupom = descontoCupom;
    }

    public Double getFrete() {
        return frete;
    }

    public void setFrete(Double frete) {
        this.frete = frete;
    }

    public Integer getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public void setPrazoEntregaDias(Integer prazoEntregaDias) {
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public Double getAjustePagamento() {
        return ajustePagamento;
    }

    public void setAjustePagamento(Double ajustePagamento) {
        this.ajustePagamento = ajustePagamento;
    }

    public Double getTotalFinal() {
        return totalFinal;
    }

    public void setTotalFinal(Double totalFinal) {
        this.totalFinal = totalFinal;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public Double getValorParcela() {
        return valorParcela;
    }

    public void setValorParcela(Double valorParcela) {
        this.valorParcela = valorParcela;
    }

}
