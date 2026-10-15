package com.loja.checkout.regiao;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Região do cliente. O enunciado é explícito: para o seguro, só a porcentagem
 * muda, a conta é a mesma. Por isso região é parâmetro (o percentual), não
 * comportamento — a conta do seguro mora no serviço.
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
        for (Regiao regiao : values()) {
            if (regiao.name().equals(nome)) {
                return Optional.of(regiao);
            }
        }
        return Optional.empty();
    }
}
