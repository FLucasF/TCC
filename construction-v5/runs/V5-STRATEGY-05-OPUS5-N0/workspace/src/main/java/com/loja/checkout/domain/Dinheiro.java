package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de valores monetarios: sempre 2 casas decimais,
 * no modo "meio para o par" (HALF_EVEN), como pedido pelo financeiro.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return centavos(base.multiply(percentual).divide(BigDecimal.valueOf(100)));
    }
}
