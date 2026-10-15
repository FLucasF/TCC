package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum Regiao implements Identificavel {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final String taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    @Override
    public String codigo() {
        return name();
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, taxaSeguro);
    }
}
