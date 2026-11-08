package br.com.marealta.plan;

import br.com.marealta.Money;

import java.math.BigDecimal;

/** Resultado da precificação: soma das diárias e desconto; o total é a diferença. */
public record Quote(BigDecimal subtotal, BigDecimal discount) {
    public static Quote withoutDiscount(BigDecimal subtotal) {
        return new Quote(subtotal, Money.ZERO);
    }

    public BigDecimal total() {
        return subtotal.subtract(discount);
    }
}
