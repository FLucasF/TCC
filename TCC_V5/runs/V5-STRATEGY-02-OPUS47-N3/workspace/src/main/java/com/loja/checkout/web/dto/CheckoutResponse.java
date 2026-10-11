package com.loja.checkout.web.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.loja.checkout.domain.Resumo;

import java.math.BigDecimal;

@JsonPropertyOrder({
        "subtotalProdutos", "descontoCupom", "frete", "prazoEntregaDias", "seguro",
        "ajustePagamento", "totalFinal", "parcelas", "valorParcela",
        "creditoProximaCompra", "brinde"
})
public class CheckoutResponse {
    public BigDecimal subtotalProdutos;
    public BigDecimal descontoCupom;
    public BigDecimal frete;
    public int prazoEntregaDias;
    public BigDecimal seguro;
    public BigDecimal ajustePagamento;
    public BigDecimal totalFinal;
    public int parcelas;
    public BigDecimal valorParcela;
    public BigDecimal creditoProximaCompra;
    public boolean brinde;

    public static CheckoutResponse de(Resumo r) {
        CheckoutResponse c = new CheckoutResponse();
        c.subtotalProdutos = r.subtotalProdutos();
        c.descontoCupom = r.descontoCupom();
        c.frete = r.frete();
        c.prazoEntregaDias = r.prazoEntregaDias();
        c.seguro = r.seguro();
        c.ajustePagamento = r.ajustePagamento();
        c.totalFinal = r.totalFinal();
        c.parcelas = r.parcelas();
        c.valorParcela = r.valorParcela();
        c.creditoProximaCompra = r.creditoProximaCompra();
        c.brinde = r.brinde();
        return c;
    }
}
