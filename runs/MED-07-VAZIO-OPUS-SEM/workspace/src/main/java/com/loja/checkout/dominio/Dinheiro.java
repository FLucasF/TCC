package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de arredondamento de dinheiro da loja: sempre 2 casas (centavos),
 * sempre "meio para o par" (2,995 -> 3,00 e 2,985 -> 2,98).
 */
public final class Dinheiro {

    public static final int CASAS = 2;
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;

    /** Precisao usada nos calculos intermediarios (juros), antes do arredondamento final. */
    public static final MathContext PRECISAO = new MathContext(34, RoundingMode.HALF_EVEN);

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS, MODO);
    }
}
