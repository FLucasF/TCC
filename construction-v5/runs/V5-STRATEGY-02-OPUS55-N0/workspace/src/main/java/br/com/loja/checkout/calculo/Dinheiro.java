package br.com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras de arredondamento de valores em dinheiro: sempre para centavos,
 * usando "meio para o par" (2,995 → 3,00; 2,985 → 2,98).
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal reais(String valor) {
        return centavos(new BigDecimal(valor));
    }

    /** Aplica uma porcentagem (ex.: 2.5 para 2,5%) sobre a base e arredonda para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal porcentagem) {
        return centavos(base.multiply(porcentagem).divide(CEM));
    }
}
