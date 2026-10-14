package loja.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Dinheiro() {
    }

    /** Arredondamento "meio para o par" em centavos. */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Valor × percentual (ex.: 0.05 para 5%), arredondado em centavos. */
    public static BigDecimal percentual(BigDecimal valor, String taxa) {
        return arredondar(valor.multiply(new BigDecimal(taxa)));
    }
}
