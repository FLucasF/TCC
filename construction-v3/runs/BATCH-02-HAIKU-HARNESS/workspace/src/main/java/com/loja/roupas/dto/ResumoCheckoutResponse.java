package com.loja.roupas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ResumoCheckoutResponse {
    @JsonProperty("subtotalProdutos")
    private BigDecimal subtotalProdutos;

    @JsonProperty("descontoCupom")
    private BigDecimal descontoCupom;

    @JsonProperty("frete")
    private BigDecimal frete;

    @JsonProperty("prazoEntregaDias")
    private Integer prazoEntregaDias;

    @JsonProperty("ajustePagamento")
    private BigDecimal ajustePagamento;

    @JsonProperty("totalFinal")
    private BigDecimal totalFinal;

    @JsonProperty("parcelas")
    private Integer parcelas;

    @JsonProperty("valorParcela")
    private BigDecimal valorParcela;

    public ResumoCheckoutResponse() {}

    public ResumoCheckoutResponse(BigDecimal subtotalProdutos, BigDecimal descontoCupom, BigDecimal frete,
                                   Integer prazoEntregaDias, BigDecimal ajustePagamento, BigDecimal totalFinal,
                                   Integer parcelas, BigDecimal valorParcela) {
        this.subtotalProdutos = subtotalProdutos;
        this.descontoCupom = descontoCupom;
        this.frete = frete;
        this.prazoEntregaDias = prazoEntregaDias;
        this.ajustePagamento = ajustePagamento;
        this.totalFinal = totalFinal;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
    }

    public BigDecimal getSubtotalProdutos() {
        return subtotalProdutos;
    }

    public BigDecimal getDescontoCupom() {
        return descontoCupom;
    }

    public BigDecimal getFrete() {
        return frete;
    }

    public Integer getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public BigDecimal getAjustePagamento() {
        return ajustePagamento;
    }

    public BigDecimal getTotalFinal() {
        return totalFinal;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public void setSubtotalProdutos(BigDecimal subtotalProdutos) {
        this.subtotalProdutos = subtotalProdutos;
    }

    public void setDescontoCupom(BigDecimal descontoCupom) {
        this.descontoCupom = descontoCupom;
    }

    public void setFrete(BigDecimal frete) {
        this.frete = frete;
    }

    public void setPrazoEntregaDias(Integer prazoEntregaDias) {
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public void setAjustePagamento(BigDecimal ajustePagamento) {
        this.ajustePagamento = ajustePagamento;
    }

    public void setTotalFinal(BigDecimal totalFinal) {
        this.totalFinal = totalFinal;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public void setValorParcela(BigDecimal valorParcela) {
        this.valorParcela = valorParcela;
    }
}
