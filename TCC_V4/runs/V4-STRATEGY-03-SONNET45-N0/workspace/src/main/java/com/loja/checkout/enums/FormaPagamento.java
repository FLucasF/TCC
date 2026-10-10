package com.loja.checkout.enums;

public enum FormaPagamento {
    PIX(1, 1),
    BOLETO(1, 1),
    CARTAO(1, 12);

    private final int parcelasMinimas;
    private final int parcelasMaximas;

    FormaPagamento(int parcelasMinimas, int parcelasMaximas) {
        this.parcelasMinimas = parcelasMinimas;
        this.parcelasMaximas = parcelasMaximas;
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= parcelasMinimas && parcelas <= parcelasMaximas;
    }
}
