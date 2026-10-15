package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitário de dinheiro.
 *
 * <p>Todo valor em dinheiro é arredondado para centavos (2 casas) usando o
 * arredondamento "meio para o par" ({@link RoundingMode#HALF_EVEN}), conforme
 * combinado com o financeiro (ex.: 2,995 vira 3,00 e 2,985 vira 2,98).
 */
public final class Dinheiro {

    /** Escala (casas decimais) usada para todo valor monetário. */
    public static final int ESCALA = 2;

    private Dinheiro() {
    }

    /** Arredonda um valor para centavos usando meio-para-o-par. */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_EVEN);
    }

    /**
     * Calcula uma porcentagem de uma base e arredonda para centavos.
     *
     * @param base    valor sobre o qual a porcentagem é aplicada
     * @param percentual porcentagem já em fração (ex.: 0.05 para 5%)
     */
    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return centavos(base.multiply(percentual));
    }
}
