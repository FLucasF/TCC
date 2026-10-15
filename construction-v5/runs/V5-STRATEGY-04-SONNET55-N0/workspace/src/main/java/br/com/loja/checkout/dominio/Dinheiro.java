package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valor(String texto) {
        return arredondar(new BigDecimal(texto));
    }

    /** Percentual dado como fração (0.10 = 10%), arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, String fracao) {
        return arredondar(base.multiply(new BigDecimal(fracao)));
    }
}
