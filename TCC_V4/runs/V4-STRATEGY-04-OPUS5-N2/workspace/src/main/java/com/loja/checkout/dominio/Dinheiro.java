package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Arredondamento de valores em dinheiro: centavos, meio para o par.
 * Toda etapa do calculo passa por aqui, por isso mora num lugar so.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private static final int CASAS = 2;
    private static final RoundingMode MODO = RoundingMode.HALF_EVEN;

    /** Precisao usada nas contas intermediarias, antes do arredondamento final. */
    public static final MathContext PRECISAO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(CASAS, MODO);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa));
    }

    public static BigDecimal divide(BigDecimal valor, int divisor) {
        return valor.divide(BigDecimal.valueOf(divisor), CASAS, MODO);
    }
}
