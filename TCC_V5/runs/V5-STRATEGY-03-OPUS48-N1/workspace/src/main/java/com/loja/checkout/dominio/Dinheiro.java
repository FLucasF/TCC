package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de dinheiro que vale para o pedido inteiro: todo valor monetario e
 * arredondado para centavos usando "meio para o par" (HALF_EVEN). Fica num
 * lugar so para que nenhuma etapa do calculo invente o proprio arredondamento.
 */
public final class Dinheiro {

    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final int CASAS = 2;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(CASAS, MODO);
    }
}
