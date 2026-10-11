package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Moeda {
    private static final int CASAS_DECIMAIS = 2;
    private static final RoundingMode MODO_ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, MODO_ARREDONDAMENTO);
    }

    public static BigDecimal de(double valor) {
        return arredondar(BigDecimal.valueOf(valor));
    }

    public static BigDecimal de(String valor) {
        return arredondar(new BigDecimal(valor));
    }
}
