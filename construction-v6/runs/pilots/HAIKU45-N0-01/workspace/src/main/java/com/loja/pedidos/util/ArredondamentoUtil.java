package com.loja.pedidos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondamentoUtil {
    private static final int CASAS_DECIMAIS = 2;

    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
        }
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal arredondarSemLimitar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor;
    }
}
