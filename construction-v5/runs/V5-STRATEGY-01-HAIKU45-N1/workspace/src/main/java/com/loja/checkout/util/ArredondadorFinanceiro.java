package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondadorFinanceiro {
    public static double arredondarCentavos(double valor) {
        BigDecimal bd = new BigDecimal(valor);
        bd = bd.setScale(2, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}
