package shop.shipping;

import java.math.BigDecimal;

public record CarrierQuote(BigDecimal price, int etaHours) {
}
