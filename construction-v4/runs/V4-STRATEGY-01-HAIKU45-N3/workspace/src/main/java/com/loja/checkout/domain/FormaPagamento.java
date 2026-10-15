package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum FormaPagamento {
    PIX(1, 1),
    CARTAO(1, 12),
    BOLETO(1, 1);

    private final int minParcelas;
    private final int maxParcelas;

    FormaPagamento(int minParcelas, int maxParcelas) {
        this.minParcelas = minParcelas;
        this.maxParcelas = maxParcelas;
    }

    public boolean isParcelavelEm(int parcelas) {
        return parcelas >= minParcelas && parcelas <= maxParcelas;
    }

    public boolean ehDisponivelPara(BigDecimal total) {
        if (this == BOLETO) {
            return total.compareTo(BigDecimal.valueOf(1000)) <= 0;
        }
        return true;
    }
}
