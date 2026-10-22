package br.com.marealta.domain;

import java.math.BigDecimal;

/** Resultado do preço: soma das diárias, desconto e total (diárias menos desconto). */
public record Pricing(BigDecimal subtotal, BigDecimal discount) {

    public BigDecimal total() {
        return subtotal.subtract(discount);
    }
}
