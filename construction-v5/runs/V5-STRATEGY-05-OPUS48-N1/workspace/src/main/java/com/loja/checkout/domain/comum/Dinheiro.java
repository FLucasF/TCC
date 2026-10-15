package com.loja.checkout.domain.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de dinheiro que vale igual em todas as etapas do calculo: todo valor
 * monetario fica em centavos (2 casas), arredondado "meio para o par".
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
