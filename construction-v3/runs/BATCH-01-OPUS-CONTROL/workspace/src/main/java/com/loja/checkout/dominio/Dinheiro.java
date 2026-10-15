package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de arredondamento de dinheiro da loja: centavos, "meio para o par"
 * (HALF_EVEN). Ex.: 2,995 -> 3,00 e 2,985 -> 2,98.
 */
public final class Dinheiro {

    public static final int CASAS = 2;
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    /** Precisao usada nas contas intermediarias (juros), antes do arredondamento final. */
    public static final MathContext PRECISAO_CALCULO = MathContext.DECIMAL128;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(CASAS);

    private Dinheiro() {
    }

    /** Arredonda um valor monetario para centavos. */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(CASAS, MODO);
    }
}
