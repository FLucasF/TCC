package com.loja.dto;

import java.math.BigDecimal;

public class CheckoutResponseDto {
    public BigDecimal subtotalProdutos;
    public BigDecimal descontoCupom;
    public BigDecimal frete;
    public Integer prazoEntregaDias;
    public BigDecimal seguro;
    public BigDecimal ajustePagamento;
    public BigDecimal totalFinal;
    public Integer parcelas;
    public BigDecimal valorParcela;
    public BigDecimal creditoProximaCompra;
    public Boolean brinde;

    public CheckoutResponseDto(BigDecimal subtotalProdutos, BigDecimal descontoCupom,
                                BigDecimal frete, Integer prazoEntregaDias,
                                BigDecimal seguro, BigDecimal ajustePagamento,
                                BigDecimal totalFinal, Integer parcelas,
                                BigDecimal valorParcela, BigDecimal creditoProximaCompra,
                                Boolean brinde) {
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
}
