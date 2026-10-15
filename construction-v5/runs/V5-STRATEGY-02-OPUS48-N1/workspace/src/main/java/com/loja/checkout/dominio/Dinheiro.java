package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de dinheiro. É igual em todas as etapas do cálculo:
 * centavos, meio para o par (HALF_EVEN). Mora num lugar só para que
 * nenhuma etapa invente o seu próprio jeito de arredondar.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
