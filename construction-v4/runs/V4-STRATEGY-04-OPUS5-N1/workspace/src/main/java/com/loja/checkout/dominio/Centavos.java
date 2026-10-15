package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Todo valor em dinheiro e arredondado para centavos, meio para o par, em cada etapa.
 */
public final class Centavos {

    public static final int ESCALA = 2;
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final MathContext CONTEXTO_INTERMEDIARIO = new MathContext(20, MODO);
    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Centavos() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(ESCALA, MODO);
    }

    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return arredondar(valor.multiply(taxa));
    }

    public static BigDecimal dividir(BigDecimal valor, int divisor) {
        return valor.divide(BigDecimal.valueOf(divisor), ESCALA, MODO);
    }
}
