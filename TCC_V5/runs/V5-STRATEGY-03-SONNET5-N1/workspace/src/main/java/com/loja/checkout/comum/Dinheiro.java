package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Toda etapa do calculo do resumo de compra arredonda valores em dinheiro
 * para centavos usando "meio para o par" (HALF_EVEN).
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
