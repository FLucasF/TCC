package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Dinheiro {

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal de(double valor) {
        return arredondar(BigDecimal.valueOf(valor));
    }

    public static BigDecimal percentual(BigDecimal valor, double porcentagem) {
        return arredondar(valor.multiply(BigDecimal.valueOf(porcentagem / 100.0)));
    }
}
