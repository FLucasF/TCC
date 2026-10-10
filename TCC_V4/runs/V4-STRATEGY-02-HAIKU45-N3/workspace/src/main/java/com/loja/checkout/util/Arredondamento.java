package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {

    public static double arredondar(double valor) {
        return new BigDecimal(valor)
            .setScale(2, RoundingMode.HALF_EVEN)
            .doubleValue();
    }
}
