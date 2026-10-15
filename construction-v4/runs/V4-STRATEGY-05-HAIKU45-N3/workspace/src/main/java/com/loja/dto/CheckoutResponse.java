package com.loja.dto;

import java.math.BigDecimal;

public class CheckoutResponse {
    private BigDecimal subtotalProdutos;
    private BigDecimal descontoCupom;
    private BigDecimal frete;
    private int prazoEntregaDias;
    private BigDecimal seguro;
    private BigDecimal ajustePagamento;
    private BigDecimal totalFinal;
    private int parcelas;
    private BigDecimal valorParcela;
    private BigDecimal creditoProximaCompra;
    private boolean brinde;

    public CheckoutResponse(
            BigDecimal subtotalProdutos,
            BigDecimal descontoCupom,
            BigDecimal frete,
            int prazoEntregaDias,
            BigDecimal seguro,
            BigDecimal ajustePagamento,
            BigDecimal totalFinal,
            int parcelas,
            BigDecimal valorParcela,
            BigDecimal creditoProximaCompra,
            boolean brinde) {
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

    public int getPrazoEntregaDias() {
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

    public int getParcelas() {
        return parcelas;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public BigDecimal getCreditoProximaCompra() {
        return creditoProximaCompra;
    }

    public boolean isBrinde() {
        return brinde;
    }
}
