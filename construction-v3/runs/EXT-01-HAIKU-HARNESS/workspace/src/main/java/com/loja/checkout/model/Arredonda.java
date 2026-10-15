package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredonda {
    public static BigDecimal round(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
