package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regiao do cliente. O enunciado diz que o seguro tem a mesma conta em todas
 * as regioes e so muda a porcentagem, entao aqui so mora o dado (o percentual);
 * a conta do seguro fica num lugar so, no servico.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentual;

    Regiao(String percentual) {
        this.percentual = new BigDecimal(percentual);
    }

    public BigDecimal percentual() {
        return percentual;
    }

    public static Optional<Regiao> porNome(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(nome));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
