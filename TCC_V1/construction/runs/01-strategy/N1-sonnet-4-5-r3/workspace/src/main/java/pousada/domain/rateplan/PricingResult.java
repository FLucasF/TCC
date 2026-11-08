package pousada.domain.rateplan;

import java.math.BigDecimal;

public record PricingResult(
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
}
