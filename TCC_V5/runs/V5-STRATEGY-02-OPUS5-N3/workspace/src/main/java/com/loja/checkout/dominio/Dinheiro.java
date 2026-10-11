package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de dinheiro: centavos, meio para o par. Igual em toda etapa
 * do cálculo, por isso mora num lugar só.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return centavos(valor.multiply(taxa));
    }

    public static BigDecimal dividir(BigDecimal valor, int partes) {
        return valor.divide(BigDecimal.valueOf(partes), 2, RoundingMode.HALF_EVEN);
    }
}
