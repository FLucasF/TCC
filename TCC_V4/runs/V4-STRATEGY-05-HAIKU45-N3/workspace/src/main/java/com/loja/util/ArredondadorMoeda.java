package com.loja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondadorMoeda {
    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
