package br.com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Todo valor em dinheiro do resumo passa por aqui: centavos, meio para o par.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa));
    }
}
