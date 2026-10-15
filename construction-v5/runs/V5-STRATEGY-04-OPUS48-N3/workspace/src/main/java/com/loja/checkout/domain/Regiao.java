package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Seguro por região. O enunciado diz que "só a porcentagem muda, a conta é a
 * mesma": então a região carrega apenas a taxa, e o seguro é uma conta só.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxa;

    Regiao(String taxa) {
        this.taxa = new BigDecimal(taxa);
    }

    /** Porcentagem sobre o valor dos produtos, sem desconto e sem frete. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.arredonda(subtotalProdutos.multiply(taxa));
    }

    public static Optional<Regiao> fromCodigo(String codigo) {
        return Arrays.stream(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }
}
