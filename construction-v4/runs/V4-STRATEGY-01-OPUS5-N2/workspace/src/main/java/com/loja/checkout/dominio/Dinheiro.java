package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Todo valor em dinheiro vive em centavos, arredondado meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    /** Precisao de trabalho para as contas intermediarias (juros, percentuais). */
    public static final MathContext CALCULO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal arredonda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre um valor, ja' arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return arredonda(valor.multiply(taxa));
    }
}
