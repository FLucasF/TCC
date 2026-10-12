package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de arredondamento de valores em reais: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Aplica um percentual (ex.: 0.05 para 5%) sobre o valor e arredonda para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal percentual) {
        return arredondar(valor.multiply(percentual));
    }

    public static BigDecimal reais(String valor) {
        return arredondar(new BigDecimal(valor));
    }
}
