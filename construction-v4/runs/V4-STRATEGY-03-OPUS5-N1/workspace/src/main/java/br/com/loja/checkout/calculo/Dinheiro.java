package br.com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de dinheiro: centavos, meio para o par, igual em toda etapa. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre um valor, ja em centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return centavos(valor.multiply(taxa));
    }
}
