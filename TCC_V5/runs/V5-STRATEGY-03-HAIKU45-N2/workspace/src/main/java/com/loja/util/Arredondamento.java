package com.loja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {
    public static BigDecimal arredondarParaPar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal arredondarParaPar(double valor) {
        return arredondarParaPar(BigDecimal.valueOf(valor));
    }
}
