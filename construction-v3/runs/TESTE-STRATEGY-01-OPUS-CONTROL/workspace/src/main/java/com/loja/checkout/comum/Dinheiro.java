package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores monetarios: sempre 2 casas, arredondamento meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    /** Escala usada nos calculos intermediarios (antes do arredondamento final de cada etapa). */
    private static final int ESCALA_CALCULO = 12;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return centavos(base.multiply(percentual).divide(BigDecimal.valueOf(100), ESCALA_CALCULO, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal dividir(BigDecimal valor, BigDecimal divisor) {
        return valor.divide(divisor, ESCALA_CALCULO, RoundingMode.HALF_EVEN);
    }

    public static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
