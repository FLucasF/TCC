package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Regra de arredondamento de dinheiro, igual em todas as etapas do resumo. */
public final class Dinheiro {

    public static final BigDecimal ZERO = valor(BigDecimal.ZERO);

    /** Precisão de trabalho dos cálculos intermediários que não são dinheiro (juros). */
    public static final MathContext CALCULO = MathContext.DECIMAL64;

    private Dinheiro() {
    }

    /** Arredonda para centavos usando "meio para o par". */
    public static BigDecimal valor(BigDecimal bruto) {
        return bruto.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valor(String bruto) {
        return valor(new BigDecimal(bruto));
    }

    public static BigDecimal percentual(BigDecimal base, String taxa) {
        return valor(base.multiply(new BigDecimal(taxa)));
    }
}
