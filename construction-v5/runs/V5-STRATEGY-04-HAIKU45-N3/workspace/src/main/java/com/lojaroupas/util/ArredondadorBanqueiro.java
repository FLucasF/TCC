package com.lojaroupas.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondadorBanqueiro {
    public static Double arredondar(Double valor) {
        if (valor == null) {
            return 0.0;
        }
        BigDecimal bd = new BigDecimal(valor);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
