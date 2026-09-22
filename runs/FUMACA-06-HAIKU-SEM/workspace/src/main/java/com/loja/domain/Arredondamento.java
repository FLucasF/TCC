package com.loja.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {
    public static Double arredondar(Double valor) {
        if (valor == null) {
            return 0.0;
        }
        BigDecimal bd = new BigDecimal(valor);
        bd = bd.setScale(2, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}
