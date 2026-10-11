package com.loja.checkout.web;

import java.math.BigDecimal;

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
}
