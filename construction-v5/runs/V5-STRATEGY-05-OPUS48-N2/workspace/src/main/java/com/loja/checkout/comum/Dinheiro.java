package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Todo valor em dinheiro é arredondado para centavos usando "meio para o par"
 * (HALF_EVEN). Essa regra é igual em todas as etapas do cálculo, então mora
 * num lugar só.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
