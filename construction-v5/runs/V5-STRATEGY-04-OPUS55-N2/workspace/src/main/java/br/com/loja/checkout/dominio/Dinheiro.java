package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de arredondamento comuns a todos os valores em dinheiro. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private Dinheiro() {
    }

    /** Arredonda para centavos usando "meio para o par". */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Aplica uma porcentagem (ex.: 2.5 para 2,5%) e arredonda para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal porcentagem) {
        return centavos(base.multiply(porcentagem).divide(CEM));
    }
}
