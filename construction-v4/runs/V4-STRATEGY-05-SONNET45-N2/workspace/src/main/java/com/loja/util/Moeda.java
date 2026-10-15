package com.loja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Moeda {
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
