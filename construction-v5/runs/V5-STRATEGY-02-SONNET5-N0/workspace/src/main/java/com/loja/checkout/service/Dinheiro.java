package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Todo valor monetario e arredondado para centavos usando "meio para o par"
 * (HALF_EVEN), conforme definido pelo financeiro.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
