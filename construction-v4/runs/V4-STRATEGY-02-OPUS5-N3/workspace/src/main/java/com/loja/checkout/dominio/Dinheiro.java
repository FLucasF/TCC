package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de dinheiro comum a todas as etapas: valores em centavos, arredondados
 * com "meio para o par".
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre uma base, já em centavos. O percentual vem como fração (0.025 = 2,5%). */
    public static BigDecimal percentual(BigDecimal base, BigDecimal fracao) {
        return centavos(base.multiply(fracao));
    }
}
