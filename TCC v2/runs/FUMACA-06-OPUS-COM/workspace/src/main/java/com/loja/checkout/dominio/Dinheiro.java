package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Todo valor em dinheiro tem 2 casas e arredonda meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = emCentavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal emCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
