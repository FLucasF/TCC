package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
    public static double arredondarParaCentavos(double valor) {
        BigDecimal bd = new BigDecimal(valor);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
