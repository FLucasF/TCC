package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de arredondamento de valores em dinheiro: centavos, "meio para o par". */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal reais(String valor) {
        return arredondar(new BigDecimal(valor));
    }

    /** Aplica uma porcentagem (ex.: 0.05 = 5%) sobre o valor, arredondando para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return arredondar(valor.multiply(taxa));
    }
}
