package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
    private static final int CASAS_DECIMAIS = 2;

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal arredondarSemLimite(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
    }
}
