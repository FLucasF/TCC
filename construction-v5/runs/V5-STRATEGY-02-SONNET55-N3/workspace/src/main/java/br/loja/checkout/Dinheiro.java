package br.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valor(String texto) {
        return arredondar(new BigDecimal(texto));
    }

    /** Porcentagem (ex.: "0.05" = 5%) de uma base, arredondada para centavos. */
    public static BigDecimal percentual(BigDecimal base, String taxa) {
        return arredondar(base.multiply(new BigDecimal(taxa)));
    }
}
