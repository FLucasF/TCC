package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final String percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal seguroSobre(BigDecimal subtotal) {
        return Dinheiro.porcentagem(subtotal, percentualSeguro);
    }
}
