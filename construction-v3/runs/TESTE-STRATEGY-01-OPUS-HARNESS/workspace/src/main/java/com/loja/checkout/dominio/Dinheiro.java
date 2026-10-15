package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Todo valor em dinheiro vive arredondado em centavos, meio para o par. */
public final class Dinheiro {

    /** Precisao de trabalho das contas intermediarias (juros, divisoes). */
    public static final MathContext CALCULO = new MathContext(20, RoundingMode.HALF_EVEN);

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa));
    }
}
