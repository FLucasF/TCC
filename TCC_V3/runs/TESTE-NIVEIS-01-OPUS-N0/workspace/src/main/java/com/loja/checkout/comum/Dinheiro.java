package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de valores monetarios: sempre 2 casas decimais,
 * com arredondamento "meio para o par" (2,995 -> 3,00 e 2,985 -> 2,98).
 */
public final class Dinheiro {

    public static final int CASAS = 2;
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(CASAS);

    private Dinheiro() {
    }

    /** Arredonda o valor para centavos. */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS, MODO);
    }

    /** Aplica um percentual (em fracao, ex.: 0.05 para 5%) sobre o valor e arredonda. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal fracao) {
        return arredondar(valor.multiply(fracao));
    }
}
