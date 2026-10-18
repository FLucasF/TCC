package shop.domain.shipping;

import java.math.BigDecimal;

public record ShippingQuote(BigDecimal cost, int days) {
}
