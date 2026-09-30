package com.loja.checkout.dto;

import java.math.BigDecimal;

public class RespostaCheckoutDTO {
    public BigDecimal subtotalProdutos;
    public BigDecimal descontoCupom;
    public BigDecimal frete;
    public int prazoEntregaDias;
    public BigDecimal imposto;
    public BigDecimal ajustePagamento;
    public BigDecimal totalFinal;
    public int parcelas;
    public BigDecimal valorParcela;
    public BigDecimal creditoProximaCompra;
    public boolean brinde;

    public RespostaCheckoutDTO() {}

    public RespostaCheckoutDTO(BigDecimal subtotalProdutos, BigDecimal descontoCupom, BigDecimal frete,
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
}
