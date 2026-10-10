package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Percentual sobre um valor em dinheiro, ja arredondado para centavos. */
public record Percentual(BigDecimal fracao) {

    private static final BigDecimal CEM = new BigDecimal("100");

    public static Percentual de(String porcentagem) {
        return new Percentual(new BigDecimal(porcentagem).divide(CEM, Dinheiro.PRECISAO));
    }

    public BigDecimal sobre(BigDecimal valor) {
        return Dinheiro.emCentavos(valor.multiply(fracao));
    }
}
