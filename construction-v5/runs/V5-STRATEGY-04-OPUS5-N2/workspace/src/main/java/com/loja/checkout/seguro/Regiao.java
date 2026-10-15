package com.loja.checkout.seguro;

import com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/**
 * Regiao do cliente. A conta do seguro e a mesma em todas, so a porcentagem muda.
 */
public enum Regiao implements Codificado {

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
