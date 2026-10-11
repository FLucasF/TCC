package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regiao do cliente. O enunciado diz que, no seguro, "e so a porcentagem que
 * muda, a conta e a mesma em todas". Entao a regiao nao e um comportamento
 * polimorfico: e apenas a taxa que varia, guardada em cada constante.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    public BigDecimal taxaSeguro() {
        return taxaSeguro;
    }

    public static Optional<Regiao> resolver(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
