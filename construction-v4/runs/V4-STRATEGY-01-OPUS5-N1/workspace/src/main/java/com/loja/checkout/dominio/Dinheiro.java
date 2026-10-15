package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de dinheiro: toda etapa do calculo fecha em centavos,
 * com arredondamento "meio para o par" (2,995 -> 3,00 e 2,985 -> 2,98).
 */
public final class Dinheiro {

    public static final int CASAS = 2;
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(CASAS, MODO);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa));
    }

    public static BigDecimal dividir(BigDecimal valor, int divisor) {
        return valor.divide(BigDecimal.valueOf(divisor), CASAS, MODO);
    }
}
