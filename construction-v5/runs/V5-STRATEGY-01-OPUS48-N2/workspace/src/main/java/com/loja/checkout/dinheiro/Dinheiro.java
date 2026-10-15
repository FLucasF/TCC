package com.loja.checkout.dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de arredondamento comum a todas as etapas do cálculo: centavos,
 * "meio para o par" (HALF_EVEN). É igual em todo o serviço, então mora num
 * lugar só.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
