package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {
    private static final int CASAS_DECIMAIS = 2;
    private static final RoundingMode MODO = RoundingMode.HALF_EVEN;

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, MODO);
    }
}
