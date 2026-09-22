package com.loja.checkout.api;

import java.math.BigDecimal;

public class RespostaCheckout {
    public BigDecimal subtotalProdutos;
    public BigDecimal descontoCupom;
    public BigDecimal frete;
    public int prazoEntregaDias;
    public BigDecimal ajustePagamento;
    public BigDecimal totalFinal;
    public int parcelas;
    public BigDecimal valorParcela;

    public RespostaCheckout(BigDecimal subtotalProdutos, BigDecimal descontoCupom, BigDecimal frete,
                           int prazoEntregaDias, BigDecimal ajustePagamento, BigDecimal totalFinal,
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
}
