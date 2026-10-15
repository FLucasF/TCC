package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondamento {
    public static double arredondarParaCentavos(double valor) {
        BigDecimal bd = new BigDecimal(valor);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
