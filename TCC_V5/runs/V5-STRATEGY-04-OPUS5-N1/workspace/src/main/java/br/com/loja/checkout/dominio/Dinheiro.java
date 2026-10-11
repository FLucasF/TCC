package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Toda conta em dinheiro arredonda para centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    /** Precisão de trabalho das contas intermediárias, antes do arredondamento final. */
    public static final MathContext CALCULO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal de(String valor) {
        return new BigDecimal(valor);
    }

    public static BigDecimal percentual(BigDecimal percentual, BigDecimal base) {
        return centavos(base.multiply(percentual).divide(BigDecimal.valueOf(100), CALCULO));
    }
}
