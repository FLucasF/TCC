package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de dinheiro que vale para o cálculo inteiro: todo valor é arredondado
 * para centavos usando "meio para o par" (HALF_EVEN). Fica num lugar só para
 * que nenhuma etapa invente seu próprio arredondamento.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
