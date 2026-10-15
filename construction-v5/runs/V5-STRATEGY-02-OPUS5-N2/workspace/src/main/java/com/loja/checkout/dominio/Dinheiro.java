package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de dinheiro comum a todas as etapas do resumo: centavos, meio para o par.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return arredondar(base.multiply(taxa));
    }

    public static BigDecimal taxa(String valor) {
        return new BigDecimal(valor);
    }
}
