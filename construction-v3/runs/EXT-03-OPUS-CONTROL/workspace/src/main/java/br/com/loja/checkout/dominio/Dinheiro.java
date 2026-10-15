package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de arredondamento de dinheiro da loja: sempre centavos, "meio para o par"
 * (HALF_EVEN), aplicado em cada etapa do calculo.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    /** Precisao usada nos calculos intermediarios (juros, percentuais) antes do arredondamento. */
    public static final MathContext PRECISAO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa, PRECISAO));
    }

    public static BigDecimal de(String valor) {
        return new BigDecimal(valor);
    }
}
