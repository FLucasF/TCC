package com.loja.model;

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
    private int prazoEntregaDias;

    @JsonProperty("ajustePagamento")
    private BigDecimal ajustePagamento;

    @JsonProperty("totalFinal")
    private BigDecimal totalFinal;

    @JsonProperty("parcelas")
    private int parcelas;

    @JsonProperty("valorParcela")
    private BigDecimal valorParcela;

    public CheckoutResponse(BigDecimal subtotalProdutos, BigDecimal descontoCupom,
                           BigDecimal frete, int prazoEntregaDias,
                           BigDecimal ajustePagamento, BigDecimal totalFinal,
                           int parcelas, BigDecimal valorParcela) {
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

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
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
}
