package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de arredondamento de valores em dinheiro: toda etapa do calculo
 * fecha em centavos usando "meio para o par" (HALF_EVEN).
 */
public final class Dinheiro {

    public static final int CASAS = 2;
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;

    /** Precisao usada nas contas intermediarias, antes de fechar em centavos. */
    public static final MathContext PRECISAO = new MathContext(34, MODO);

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(CASAS, MODO);
    }

    /** Aplica um percentual (ex.: 2.5 para 2,5%) sobre a base e fecha em centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return centavos(base.multiply(percentual).divide(new BigDecimal("100"), PRECISAO));
    }
}
