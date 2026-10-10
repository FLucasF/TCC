package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Utilitário para lidar com valores em dinheiro.
 *
 * <p>Toda a casa financeira da loja arredonda para centavos usando o
 * arredondamento "meio para o par" ({@link RoundingMode#HALF_EVEN}),
 * conforme combinado com o financeiro (ex.: 2,995 → 3,00 e 2,985 → 2,98).
 */
public final class Dinheiro {

    /** Casas decimais de um valor em reais. */
    public static final int ESCALA = 2;

    /** Zero já na escala de centavos. */
    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    /** Arredonda um valor qualquer para centavos (meio para o par). */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_EVEN);
    }

    /** Aplica uma porcentagem (ex.: 2.5 para 2,5%) sobre uma base e arredonda. */
    public static BigDecimal porcentagem(BigDecimal base, BigDecimal percentual) {
        BigDecimal fracao = percentual.divide(BigDecimal.valueOf(100), MathContext.DECIMAL64);
        return centavos(base.multiply(fracao));
    }
}
