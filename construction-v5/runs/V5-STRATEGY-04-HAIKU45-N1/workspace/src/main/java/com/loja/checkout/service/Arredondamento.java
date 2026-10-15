package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {
    private static final RoundingMode MODO = RoundingMode.HALF_EVEN;

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, MODO);
    }
}
