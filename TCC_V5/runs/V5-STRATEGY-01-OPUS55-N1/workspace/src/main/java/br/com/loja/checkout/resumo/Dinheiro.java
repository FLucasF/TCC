package br.com.loja.checkout.resumo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de arredondamento de valores em dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Aplica um percentual (ex.: 2.5 para 2,5%) sobre o valor, arredondando para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal percentual) {
        return centavos(valor.multiply(percentual).movePointLeft(2));
    }
}
