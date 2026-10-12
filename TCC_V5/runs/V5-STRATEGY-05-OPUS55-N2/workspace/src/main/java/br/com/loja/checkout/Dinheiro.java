package br.com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de valores em dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal valor, String percentual) {
        return centavos(valor.multiply(new BigDecimal(percentual)).movePointLeft(2));
    }
}
