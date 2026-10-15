package com.loja.checkout.domain;

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

    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= minParcelas && parcelas <= maxParcelas;
    }
}
