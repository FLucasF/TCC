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

    public CheckoutResponse(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        Integer prazoEntregaDias,
        BigDecimal seguro,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        Integer parcelas,
        BigDecimal valorParcela,
        BigDecimal creditoProximaCompra,
        Boolean brinde
    ) {
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
