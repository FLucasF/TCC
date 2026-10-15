package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal arredondar(double valor) {
        return arredondar(BigDecimal.valueOf(valor));
    }
}
