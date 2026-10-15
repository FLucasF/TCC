package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
    private static final int CASAS_DECIMAIS = 2;

    public static Double arredondar(Double valor) {
        if (valor == null) return 0.0;
        BigDecimal bd = new BigDecimal(valor);
        bd = bd.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }

    public static Double arredondar(BigDecimal valor) {
        if (valor == null) return 0.0;
        valor = valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
        return valor.doubleValue();
    }
}
