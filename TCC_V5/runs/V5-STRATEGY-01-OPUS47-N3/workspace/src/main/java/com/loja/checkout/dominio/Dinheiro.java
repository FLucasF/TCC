package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final BigDecimal ZERO = aCentavos(BigDecimal.ZERO);

    private Dinheiro() {}

    public static BigDecimal aCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
