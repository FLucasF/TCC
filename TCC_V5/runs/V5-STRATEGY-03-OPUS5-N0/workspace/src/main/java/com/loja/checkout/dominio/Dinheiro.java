package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra unica de arredondamento do servico: todo valor em dinheiro fica com
 * duas casas decimais, arredondado com "meio para o par" (HALF_EVEN).
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    private Dinheiro() {
    }

    public static BigDecimal arredonda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal valor, BigDecimal percentual) {
        return arredonda(valor.multiply(percentual).divide(BigDecimal.valueOf(100)));
    }
}
