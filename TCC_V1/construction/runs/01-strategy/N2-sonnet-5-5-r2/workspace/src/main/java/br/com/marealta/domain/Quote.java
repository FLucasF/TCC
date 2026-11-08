package br.com.marealta.domain;

import java.math.BigDecimal;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount) {
    public BigDecimal total() {
        return Money.round(subtotal.subtract(discount));
    }
}
