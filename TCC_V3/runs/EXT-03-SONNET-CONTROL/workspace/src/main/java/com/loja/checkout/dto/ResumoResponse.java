package com.loja.checkout.dto;

import java.math.BigDecimal;

public class ResumoResponse {

    private final BigDecimal subtotalProdutos;
    private final BigDecimal descontoCupom;
    private final BigDecimal frete;
    private final int prazoEntregaDias;
    private final BigDecimal imposto;
    private final BigDecimal ajustePagamento;
    private final BigDecimal totalFinal;
    private final int parcelas;
    private final BigDecimal valorParcela;
    private final BigDecimal creditoProximaCompra;
    private final boolean brinde;

    public ResumoResponse(BigDecimal subtotalProdutos, BigDecimal descontoCupom, BigDecimal frete,
                           int prazoEntregaDias, BigDecimal imposto, BigDecimal ajustePagamento,
                           BigDecimal totalFinal, int parcelas, BigDecimal valorParcela,
                           BigDecimal creditoProximaCompra, boolean brinde) {
        this.subtotalProdutos = subtotalProdutos;
        this.descontoCupom = descontoCupom;
        this.frete = frete;
        this.prazoEntregaDias = prazoEntregaDias;
        this.imposto = imposto;
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

    public BigDecimal getImposto() {
        return imposto;
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
