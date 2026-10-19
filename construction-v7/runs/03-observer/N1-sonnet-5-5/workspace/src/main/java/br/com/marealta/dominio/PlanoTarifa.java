package br.com.marealta.dominio;

import java.math.BigDecimal;

public enum PlanoTarifa {
    STANDARD("0", true),
    NON_REFUNDABLE("0.15", false);

    private final BigDecimal desconto;
    private final boolean reembolsavel;

    PlanoTarifa(String desconto, boolean reembolsavel) {
        this.desconto = new BigDecimal(desconto);
        this.reembolsavel = reembolsavel;
    }

    /** Desconto sobre a soma das noites, já arredondado em centavos. */
    public BigDecimal descontoSobre(BigDecimal subtotal) {
        return Dinheiro.arredondar(subtotal.multiply(desconto));
    }

    public boolean reembolsavel() {
        return reembolsavel;
    }
}
