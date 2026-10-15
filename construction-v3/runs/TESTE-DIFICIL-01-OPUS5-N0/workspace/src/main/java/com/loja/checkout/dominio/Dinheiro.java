package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de valores em dinheiro: sempre 2 casas decimais,
 * com a regra "meio para o par" (2,995 -> 3,00 e 2,985 -> 2,98).
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return centavos(base.multiply(percentual).divide(BigDecimal.valueOf(100)));
    }
}
