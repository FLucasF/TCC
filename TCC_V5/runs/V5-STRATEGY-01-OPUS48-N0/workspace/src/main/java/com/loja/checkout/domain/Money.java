package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitario para lidar com dinheiro. Todo valor em dinheiro do pedido e
 * arredondado para centavos (2 casas) usando "meio para o par" (HALF_EVEN),
 * conforme definido pelo financeiro. Ex.: 2,995 -> 3,00 e 2,985 -> 2,98.
 */
public final class Money {

    private Money() {
    }

    /** Arredonda para centavos usando HALF_EVEN. */
    public static BigDecimal cents(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal zero() {
        return cents(BigDecimal.ZERO);
    }
}
