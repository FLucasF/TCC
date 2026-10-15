package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class CheckoutResponse {
    @JsonProperty("subtotalProdutos")
    private BigDecimal subtotalProdutos;

    @JsonProperty("descontoCupom")
    private BigDecimal descontoCupom;

    @JsonProperty("frete")
    private BigDecimal frete;

    @JsonProperty("prazoEntregaDias")
    private Integer prazoEntregaDias;

    @JsonProperty("seguro")
    private BigDecimal seguro;

    @JsonProperty("ajustePagamento")
    private BigDecimal ajustePagamento;

    @JsonProperty("totalFinal")
    private BigDecimal totalFinal;

    @JsonProperty("parcelas")
    private Integer parcelas;

    @JsonProperty("valorParcela")
    private BigDecimal valorParcela;

    @JsonProperty("creditoProximaCompra")
    private BigDecimal creditoProximaCompra;

    @JsonProperty("brinde")
    private Boolean brinde;

    public CheckoutResponse() {}

    public BigDecimal getSubtotalProdutos() {
        return subtotalProdutos;
    }

    public void setSubtotalProdutos(BigDecimal subtotalProdutos) {
        this.subtotalProdutos = subtotalProdutos;
    }

    public BigDecimal getDescontoCupom() {
        return descontoCupom;
    }

    public void setDescontoCupom(BigDecimal descontoCupom) {
        this.descontoCupom = descontoCupom;
    }

    public BigDecimal getFrete() {
        return frete;
    }

    public void setFrete(BigDecimal frete) {
        this.frete = frete;
    }

    public Integer getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public void setPrazoEntregaDias(Integer prazoEntregaDias) {
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public BigDecimal getSeguro() {
        return seguro;
    }

    public void setSeguro(BigDecimal seguro) {
        this.seguro = seguro;
    }

    public BigDecimal getAjustePagamento() {
        return ajustePagamento;
    }

    public void setAjustePagamento(BigDecimal ajustePagamento) {
        this.ajustePagamento = ajustePagamento;
    }

    public BigDecimal getTotalFinal() {
        return totalFinal;
    }

    public void setTotalFinal(BigDecimal totalFinal) {
        this.totalFinal = totalFinal;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public void setValorParcela(BigDecimal valorParcela) {
        this.valorParcela = valorParcela;
    }

    public BigDecimal getCreditoProximaCompra() {
        return creditoProximaCompra;
    }

    public void setCreditoProximaCompra(BigDecimal creditoProximaCompra) {
        this.creditoProximaCompra = creditoProximaCompra;
    }

    public Boolean getBrinde() {
        return brinde;
    }

    public void setBrinde(Boolean brinde) {
        this.brinde = brinde;
    }
}
