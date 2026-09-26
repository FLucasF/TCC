package com.loja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondadorMeioParaPar {
    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
