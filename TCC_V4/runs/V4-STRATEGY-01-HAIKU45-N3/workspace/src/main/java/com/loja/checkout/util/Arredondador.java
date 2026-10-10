package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
    public static BigDecimal arredondarParaCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
