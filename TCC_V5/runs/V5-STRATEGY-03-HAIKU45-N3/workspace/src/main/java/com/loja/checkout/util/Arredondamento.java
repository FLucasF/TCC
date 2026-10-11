package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {

    public static BigDecimal arredondarParaCentavos(BigDecimal valor) {
        if (valor == null) {
            return new BigDecimal("0.00");
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
