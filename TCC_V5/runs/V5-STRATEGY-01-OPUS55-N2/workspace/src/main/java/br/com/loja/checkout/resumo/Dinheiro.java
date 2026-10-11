package br.com.loja.checkout.resumo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Todo valor em dinheiro é arredondado para centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, String percentual) {
        return centavos(base.multiply(new BigDecimal(percentual)).movePointLeft(2));
    }
}
