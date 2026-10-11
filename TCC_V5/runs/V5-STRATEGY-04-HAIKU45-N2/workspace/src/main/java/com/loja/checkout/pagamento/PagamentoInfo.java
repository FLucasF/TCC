package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public class PagamentoInfo {
    public BigDecimal ajuste;
    public int parcelas;
    public BigDecimal valorParcela;

    public PagamentoInfo(BigDecimal ajuste, int parcelas, BigDecimal valorParcela) {
        this.ajuste = ajuste;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
    }
}
