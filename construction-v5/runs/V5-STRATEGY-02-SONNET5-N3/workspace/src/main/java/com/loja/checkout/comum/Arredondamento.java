package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Arredondamento {

    public static final BigDecimal ZERO_CENTAVOS = new BigDecimal("0.00");

    private Arredondamento() {
    }

    public static BigDecimal paraCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
