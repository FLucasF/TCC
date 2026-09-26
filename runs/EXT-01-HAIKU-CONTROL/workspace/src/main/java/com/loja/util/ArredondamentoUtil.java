package com.loja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondamentoUtil {

    public static BigDecimal arredondarMeioParaPar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal arredondarParaCentavos(Double valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return arredondarMeioParaPar(BigDecimal.valueOf(valor));
    }
}
