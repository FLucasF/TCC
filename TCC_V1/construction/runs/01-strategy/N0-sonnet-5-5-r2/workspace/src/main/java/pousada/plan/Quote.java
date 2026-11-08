package pousada.plan;

import pousada.Money;

import java.math.BigDecimal;

/** subtotal = soma das noites; total = subtotal - desconto. */
public record Quote(BigDecimal subtotal, BigDecimal discount) {
    public Quote {
        subtotal = Money.round(subtotal);
        discount = Money.round(discount);
    }

    public static Quote withDiscountRate(BigDecimal subtotal, BigDecimal rate) {
        return new Quote(subtotal, Money.times(subtotal, rate));
    }

    public BigDecimal total() {
        return Money.round(subtotal.subtract(discount));
    }
}
