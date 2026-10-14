package br.com.loja.pedidos.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores em reais, sempre com 2 casas e arredondamento "meio para o par". */
public final class Dinheiro {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Dinheiro() {
    }

    public static BigDecimal reais(String valor) {
        return arredondar(new BigDecimal(valor));
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** {@code percentual}% de {@code base}, arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return arredondar(base.multiply(percentual).movePointLeft(2));
    }

    public static BigDecimal menor(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
