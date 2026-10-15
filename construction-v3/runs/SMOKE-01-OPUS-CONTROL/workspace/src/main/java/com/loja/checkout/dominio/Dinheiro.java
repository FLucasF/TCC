package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Regras de arredondamento de dinheiro: sempre centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = valor(BigDecimal.ZERO);

    /** Precisao usada nos calculos intermediarios (juros), antes do arredondamento final. */
    public static final MathContext CALCULO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal valor(BigDecimal bruto) {
        return bruto.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valor(String bruto) {
        return valor(new BigDecimal(bruto));
    }
}
