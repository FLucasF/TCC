package com.loja.estrategia;

import java.math.BigDecimal;

public class AjustePagamentoResult {
    public BigDecimal ajuste;
    public int parcelas;
    public BigDecimal valorParcela;

    public AjustePagamentoResult(BigDecimal ajuste, int parcelas, BigDecimal valorParcela) {
        this.ajuste = ajuste;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
    }
}
