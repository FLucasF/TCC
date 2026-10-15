package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regra de dinheiro que vale para todo o calculo: valores em centavos,
 * arredondados meio para o par em cada etapa.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = emCentavos(BigDecimal.ZERO);

    /** Precisao usada nas contas intermediarias, antes do arredondamento para centavos. */
    public static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);

    private Dinheiro() {
    }

    public static BigDecimal emCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal reais(String valor) {
        return emCentavos(new BigDecimal(valor));
    }
}
