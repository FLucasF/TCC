package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento monetario padrao do negocio: "meio para o par" (HALF_EVEN),
 * sempre com 2 casas decimais.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal de(String valor) {
        return arredondar(new BigDecimal(valor));
    }
}
