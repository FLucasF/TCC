package br.com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento único da loja: centavos, meio para o par. */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
