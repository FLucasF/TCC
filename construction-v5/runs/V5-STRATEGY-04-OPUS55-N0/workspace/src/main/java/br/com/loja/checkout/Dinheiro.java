package br.com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de arredondamento de valores em dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal reais(String valor) {
        return new BigDecimal(valor).setScale(2);
    }

    /** Aplica um percentual (ex.: 0.05 para 5%) e arredonda para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return centavos(valor.multiply(taxa));
    }
}
