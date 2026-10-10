package br.com.loja.checkout.pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de arredondamento de valores em reais: centavos, "meio para o par". */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal reais(String valor) {
        return arredondar(new BigDecimal(valor));
    }

    /** Aplica uma taxa (ex.: 0.05 para 5%) sobre a base, já arredondando para centavos. */
    public static BigDecimal aplicarTaxa(BigDecimal base, BigDecimal taxa) {
        return arredondar(base.multiply(taxa));
    }
}
