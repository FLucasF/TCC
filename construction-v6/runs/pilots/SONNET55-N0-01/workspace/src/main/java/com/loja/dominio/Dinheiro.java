package com.loja.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento monetário: centavos, "meio para o par". */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal de(String valor) {
        return arredondar(new BigDecimal(valor));
    }

    /** Porcentagem (ex.: "0.10") de uma base, arredondada para centavos. */
    public static BigDecimal porcentagem(BigDecimal base, String taxa) {
        return arredondar(base.multiply(new BigDecimal(taxa)));
    }
}
