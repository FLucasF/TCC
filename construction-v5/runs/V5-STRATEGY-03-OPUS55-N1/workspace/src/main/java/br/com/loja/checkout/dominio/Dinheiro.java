package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
