package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de dinheiro: sempre centavos, "meio para o par". */
public final class Dinheiro {

    public static final BigDecimal ZERO = valor("0");

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valor(String texto) {
        return arredondar(new BigDecimal(texto));
    }

    /** Percentual (ex.: "2.5" para 2,5%) sobre uma base, já arredondado. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return arredondar(base.multiply(percentual).divide(new BigDecimal("100")));
    }
}
