package com.loja.checkout.dto;

import java.math.BigDecimal;

public class RespostaCheckout {
    private BigDecimal subtotalProdutos;
    private BigDecimal descontoCupom;
    private BigDecimal frete;
    private Integer prazoEntregaDias;
    private BigDecimal seguro;
    private BigDecimal ajustePagamento;
    private BigDecimal totalFinal;
    private Integer parcelas;
    private BigDecimal valorParcela;
    private BigDecimal creditoProximaCompra;
    private Boolean brinde;

    public RespostaCheckout(BigDecimal subtotalProdutos, BigDecimal descontoCupom, BigDecimal frete,
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
