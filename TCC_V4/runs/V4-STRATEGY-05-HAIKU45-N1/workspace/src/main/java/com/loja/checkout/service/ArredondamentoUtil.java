package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondamentoUtil {
    private static final int CASAS_DECIMAIS = 2;

    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal arredondar(double valor) {
        return arredondar(BigDecimal.valueOf(valor));
    }
}
