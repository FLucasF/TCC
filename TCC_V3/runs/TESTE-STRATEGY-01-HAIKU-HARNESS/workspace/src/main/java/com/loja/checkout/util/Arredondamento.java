package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {
    public static BigDecimal arredondarMeioParaPar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
