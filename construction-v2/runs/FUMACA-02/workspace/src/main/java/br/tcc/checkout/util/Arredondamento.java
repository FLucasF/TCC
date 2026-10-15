package br.tcc.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {
    private static final int CASAS_DECIMAIS = 2;

    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
    }
}
