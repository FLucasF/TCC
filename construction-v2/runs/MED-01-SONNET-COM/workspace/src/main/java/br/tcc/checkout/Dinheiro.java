package br.tcc.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Dinheiro {

    private Dinheiro() {
    }

    static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
