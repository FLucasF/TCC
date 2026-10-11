package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitarios de dinheiro. Todo valor monetario do resumo tem 2 casas decimais
 * e usa arredondamento "meio para o par" (HALF_EVEN).
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre uma base, ja arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return arredondar(base.multiply(percentual));
    }

    public static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
