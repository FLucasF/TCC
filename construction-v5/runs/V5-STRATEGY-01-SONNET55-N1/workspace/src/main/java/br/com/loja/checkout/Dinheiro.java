package br.com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public static BigDecimal percentual(BigDecimal base, String taxa) {
        return arredondar(base.multiply(new BigDecimal(taxa)));
    }
}
