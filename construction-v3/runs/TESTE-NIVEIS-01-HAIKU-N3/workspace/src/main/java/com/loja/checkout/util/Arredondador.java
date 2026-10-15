package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
    private static final int CASAS_DECIMAIS = 2;
    private static final RoundingMode MODO = RoundingMode.HALF_EVEN;

    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(CASAS_DECIMAIS, MODO);
    }

    public static BigDecimal arredondar(Double valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return arredondar(BigDecimal.valueOf(valor));
    }
}
