package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Aritmética de dinheiro: centavos, arredondamento "meio para o par". */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valor(String texto) {
        return arredondar(new BigDecimal(texto));
    }

    /** base × fração (ex.: 0.10 para 10%), arredondado para centavos. */
    public static BigDecimal fracao(BigDecimal base, BigDecimal fracao) {
        return arredondar(base.multiply(fracao));
    }

    public static BigDecimal minimo(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
