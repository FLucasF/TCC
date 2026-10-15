package br.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de valores em reais: centavos, meio para o par. */
public final class Dinheiro {
    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String valor) {
        return arredondar(new BigDecimal(valor));
    }

    /** Percentual (ex.: "0.05" para 5%) aplicado sobre um valor, já arredondado. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return arredondar(valor.multiply(taxa));
    }
}
