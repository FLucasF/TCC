package pousada.domain;

import java.math.BigDecimal;

public record Quote(BigDecimal subtotal, BigDecimal discount) {
    public BigDecimal total() {
        return Money.round(subtotal.subtract(discount));
    }
}
