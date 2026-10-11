package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Regras de arredondamento de valores em dinheiro (centavos, meio para o par). */
public final class Dinheiro {

    public static final int CASAS_DECIMAIS = 2;
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(CASAS_DECIMAIS);

    /** Precisão usada nos cálculos intermediários (juros, percentuais). */
    public static final MathContext CALCULO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    /** Arredonda para centavos usando "meio para o par". */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, MODO);
    }

    /** Percentual sobre uma base, já arredondado para centavos. Ex.: percentual(100, "0.05") = 5.00 */
    public static BigDecimal percentual(BigDecimal base, BigDecimal fracao) {
        return arredondar(base.multiply(fracao));
    }
}
