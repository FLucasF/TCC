package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Arredondamento {

    private Arredondamento() {
    }

    public static BigDecimal paraCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
