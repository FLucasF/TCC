package loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal fracao) {
        return arredondar(base.multiply(fracao));
    }
}
