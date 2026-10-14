package com.loja.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ResumoDTO {
    @JsonProperty("subtotalProdutos")
    public BigDecimal subtotalProdutos;

    @JsonProperty("descontoCupom")
    public BigDecimal descontoCupom;

    @JsonProperty("frete")
    public BigDecimal frete;

    @JsonProperty("prazoEntregaDias")
    public int prazoEntregaDias;

    @JsonProperty("seguro")
    public BigDecimal seguro;

    @JsonProperty("ajustePagamento")
    public BigDecimal ajustePagamento;

    @JsonProperty("totalFinal")
    public BigDecimal totalFinal;

    @JsonProperty("parcelas")
    public int parcelas;

    @JsonProperty("valorParcela")
    public BigDecimal valorParcela;

    @JsonProperty("creditoProximaCompra")
    public BigDecimal creditoProximaCompra;

    @JsonProperty("brinde")
    public boolean brinde;

    public ResumoDTO() {}

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

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public void setPrazoEntregaDias(int prazoEntregaDias) {
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

    public int getParcelas() {
        return parcelas;
    }

    public void setParcelas(int parcelas) {
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

    public boolean isBrinde() {
        return brinde;
    }

    public void setBrinde(boolean brinde) {
        this.brinde = brinde;
    }
}
