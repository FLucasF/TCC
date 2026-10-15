package com.loja.checkout.dto;

import java.math.BigDecimal;

public class CheckoutResponse {
    private BigDecimal subtotalProdutos;
    private BigDecimal descontoCupom;
    private BigDecimal frete;
    private Integer prazoEntregaDias;
    private BigDecimal ajustePagamento;
    private BigDecimal totalFinal;
    private Integer parcelas;
    private BigDecimal valorParcela;

    public CheckoutResponse(BigDecimal subtotalProdutos, BigDecimal descontoCupom, BigDecimal frete,
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
}
