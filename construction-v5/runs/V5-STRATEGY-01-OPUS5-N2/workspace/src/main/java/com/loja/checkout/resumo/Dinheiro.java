package com.loja.checkout.resumo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Arredondamento de valores em dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    /** Precisão de trabalho das contas intermediárias, antes do arredondamento final. */
    public static final MathContext CALCULO = new MathContext(20, RoundingMode.HALF_EVEN);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
