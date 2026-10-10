package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutResponse {
    @JsonProperty("subtotalProdutos")
    private Double subtotalProdutos;

    @JsonProperty("descontoCupom")
    private Double descontoCupom;

    @JsonProperty("frete")
    private Double frete;

    @JsonProperty("prazoEntregaDias")
    private Integer prazoEntregaDias;

    @JsonProperty("seguro")
    private Double seguro;

    @JsonProperty("ajustePagamento")
    private Double ajustePagamento;

    @JsonProperty("totalFinal")
    private Double totalFinal;

    @JsonProperty("parcelas")
    private Integer parcelas;

    @JsonProperty("valorParcela")
    private Double valorParcela;

    @JsonProperty("creditoProximaCompra")
    private Double creditoProximaCompra;

    @JsonProperty("brinde")
    private Boolean brinde;

    @JsonProperty("erro")
    private String erro;

    public CheckoutResponse() {}

    public static CheckoutResponse error(String erro) {
        CheckoutResponse response = new CheckoutResponse();
        response.erro = erro;
        return response;
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

    public Double getSeguro() {
        return seguro;
    }

    public void setSeguro(Double seguro) {
        this.seguro = seguro;
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

    public Double getCreditoProximaCompra() {
        return creditoProximaCompra;
    }

    public void setCreditoProximaCompra(Double creditoProximaCompra) {
        this.creditoProximaCompra = creditoProximaCompra;
    }

    public Boolean getBrinde() {
        return brinde;
    }

    public void setBrinde(Boolean brinde) {
        this.brinde = brinde;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
