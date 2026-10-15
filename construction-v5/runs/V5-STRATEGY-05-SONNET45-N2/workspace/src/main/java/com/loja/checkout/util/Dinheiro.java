package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Dinheiro {

    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }
}
