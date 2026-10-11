package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento monetario do sistema: centavos, "meio para o par" (HALF_EVEN).
 * E a unica regra que vale igual em todas as etapas do calculo, entao mora aqui.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Porcentagem de uma base, ja arredondada: ex. percentual=2.5 -> 2,5% da base. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return centavos(base.multiply(percentual).movePointLeft(2));
    }
}
