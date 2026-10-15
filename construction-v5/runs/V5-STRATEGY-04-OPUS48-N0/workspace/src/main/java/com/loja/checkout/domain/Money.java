package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Utilidades de dinheiro.
 *
 * <p>Toda a loja trabalha em reais e arredonda os valores para centavos usando o
 * arredondamento "meio para o par" ({@link RoundingMode#HALF_EVEN}), conforme combinado
 * com o financeiro (ex.: 2,995 -> 3,00 e 2,985 -> 2,98).
 */
public final class Money {

    /** Precisão alta para os cálculos intermediários (juros, percentuais). */
    public static final MathContext MC = MathContext.DECIMAL128;

    private Money() {
    }

    /** Arredonda um valor para 2 casas decimais (centavos), meio para o par. */
    public static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Aplica um percentual sobre uma base e arredonda para centavos. */
    public static BigDecimal percent(BigDecimal base, BigDecimal percent) {
        return cents(base.multiply(percent, MC).divide(BigDecimal.valueOf(100), MC));
    }
}
