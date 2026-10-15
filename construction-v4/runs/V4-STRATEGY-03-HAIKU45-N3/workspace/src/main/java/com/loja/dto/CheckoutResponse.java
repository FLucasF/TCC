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
