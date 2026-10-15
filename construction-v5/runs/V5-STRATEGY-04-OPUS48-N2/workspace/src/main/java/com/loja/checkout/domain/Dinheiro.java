package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra única de dinheiro do sistema: todo valor em centavos, arredondado
 * "meio para o par". Fica num lugar só porque não muda de etapa para etapa.
 */
public final class Dinheiro {

    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    /** Arredonda para centavos usando meio-para-o-par. */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, MODO);
    }

    /** Percentual (ex.: 0.10) sobre uma base, já arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa));
    }
}
