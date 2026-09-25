package com.loja.roupas.domain.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
