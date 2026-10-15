package com.loja.checkout.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {
    private static final int CASAS_DECIMAIS = 2;

    public static BigDecimal arredondarParaCentavos(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
    }
}
