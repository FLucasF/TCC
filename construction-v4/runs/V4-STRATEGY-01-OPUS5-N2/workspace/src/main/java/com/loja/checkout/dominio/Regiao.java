package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Onde o cliente mora. Da regiao so' muda a taxa do seguro: a conta do seguro
 * e' a mesma em todas, feita em {@link com.loja.checkout.dominio.CalculadoraResumo}.
 */
public enum Regiao implements Identificavel {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    @Override
    public String codigo() {
        return name();
    }

    public BigDecimal taxaSeguro() {
        return taxaSeguro;
    }
}
