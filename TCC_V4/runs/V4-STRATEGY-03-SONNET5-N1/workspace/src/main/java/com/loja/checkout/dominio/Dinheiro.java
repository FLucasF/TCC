package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento "meio para o par" em centavos, usado em toda etapa do cálculo. */
public final class Dinheiro {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
