package br.com.mareal.plans;

import br.com.mareal.domain.Money;
import java.math.BigDecimal;

public record Pricing(BigDecimal subtotal, BigDecimal discount) {
    public static Pricing withDiscount(BigDecimal subtotal, BigDecimal rate) {
        return new Pricing(subtotal, Money.percent(subtotal, rate));
    }

    public static Pricing noDiscount(BigDecimal subtotal) {
        return new Pricing(subtotal, Money.ZERO);
    }

    public BigDecimal total() {
        return Money.round(subtotal.subtract(discount));
    }
}
