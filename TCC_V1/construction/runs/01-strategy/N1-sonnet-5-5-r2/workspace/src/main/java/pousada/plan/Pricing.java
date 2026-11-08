package pousada.plan;

import pousada.domain.Money;

import java.math.BigDecimal;

public record Pricing(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    public static Pricing of(BigDecimal subtotal, BigDecimal discount) {
        return new Pricing(subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
