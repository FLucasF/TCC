package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredonda {
    public static BigDecimal arredonda(double valor) {
        return arredonda(new BigDecimal(String.valueOf(valor)));
    }

    public static BigDecimal arredonda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal zero() {
        return new BigDecimal("0.00");
    }
}
