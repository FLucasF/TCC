package br.com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    /** Arredonda para centavos, meio para o par. */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, String taxa) {
        return arredondar(base.multiply(new BigDecimal(taxa)));
    }
}
