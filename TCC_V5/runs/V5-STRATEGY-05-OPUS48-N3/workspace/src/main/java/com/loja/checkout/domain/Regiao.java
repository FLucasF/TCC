package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Regiões do cliente. Só a porcentagem do seguro muda de uma para outra; a
 * conta é a mesma (porcentagem sobre o valor dos produtos). Por isso é só um
 * campo, não uma estratégia.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    /** Seguro do envio, sobre o valor dos produtos, arredondado para centavos. */
    public BigDecimal seguro(BigDecimal subtotal) {
        return Dinheiro.centavos(subtotal.multiply(percentualSeguro));
    }
}
