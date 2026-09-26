package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de arredondamento do dinheiro: toda etapa do calculo fecha em centavos
 * usando "meio para o par" (2,995 -> 3,00 e 2,985 -> 2,98).
 */
public final class Dinheiro {

    public static final int CASAS = 2;
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(CASAS);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(CASAS, MODO);
    }

    public static BigDecimal centavos(String valor) {
        return centavos(new BigDecimal(valor));
    }
}
