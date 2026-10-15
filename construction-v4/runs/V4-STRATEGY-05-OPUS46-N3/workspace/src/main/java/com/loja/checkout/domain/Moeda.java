package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Moeda {

    private Moeda() {}

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
