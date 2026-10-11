package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Arredondamento {
    private Arredondamento() {}

    public static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
