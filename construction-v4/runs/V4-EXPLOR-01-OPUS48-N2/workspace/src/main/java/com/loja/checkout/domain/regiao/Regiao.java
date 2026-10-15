package com.loja.checkout.domain.regiao;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Região do cliente. Aqui só a porcentagem do seguro muda de uma para outra;
 * a conta é a mesma em todas (porcentagem sobre o valor dos produtos), então
 * a região é apenas o dado da porcentagem, não um comportamento próprio.
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

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    public static Optional<Regiao> buscar(String codigo) {
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
