package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Região do cliente. O enunciado é explícito: "é só a porcentagem que muda, a
 * conta é a mesma em todas". Por isso a região não é um caso polimórfico — guarda
 * apenas o percentual do seguro; a conta do seguro mora na calculadora.
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

    public static Optional<Regiao> porNome(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(nome));
        } catch (IllegalArgumentException naoExiste) {
            return Optional.empty();
        }
    }
}
