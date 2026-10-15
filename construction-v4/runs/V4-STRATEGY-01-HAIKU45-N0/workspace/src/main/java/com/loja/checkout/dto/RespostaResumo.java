package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class RespostaResumo {
    @JsonProperty
    private BigDecimal subtotalProdutos;
    @JsonProperty
    private BigDecimal descontoCupom;
    @JsonProperty
    private BigDecimal frete;
    @JsonProperty
    private Integer prazoEntregaDias;
    @JsonProperty
    private BigDecimal seguro;
    @JsonProperty
    private BigDecimal ajustePagamento;
    @JsonProperty
    private BigDecimal totalFinal;
    @JsonProperty
    private Integer parcelas;
    @JsonProperty
    private BigDecimal valorParcela;
    @JsonProperty
    private BigDecimal creditoProximaCompra;
    @JsonProperty
    private Boolean brinde;

    public RespostaResumo(BigDecimal subtotalProdutos, BigDecimal descontoCupom, BigDecimal frete,
                          Integer prazoEntregaDias, BigDecimal seguro, BigDecimal ajustePagamento,
                          BigDecimal totalFinal, Integer parcelas, BigDecimal valorParcela,
                          BigDecimal creditoProximaCompra, Boolean brinde) {
        this.subtotalProdutos = subtotalProdutos;
        this.descontoCupom = descontoCupom;
        this.frete = frete;
        this.prazoEntregaDias = prazoEntregaDias;
        this.seguro = seguro;
        this.ajustePagamento = ajustePagamento;
        this.totalFinal = totalFinal;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
        this.creditoProximaCompra = creditoProximaCompra;
        this.brinde = brinde;
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

    public BigDecimal getSeguro() {
        return seguro;
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

    public BigDecimal getCreditoProximaCompra() {
        return creditoProximaCompra;
    }

    public Boolean getBrinde() {
        return brinde;
    }
}
