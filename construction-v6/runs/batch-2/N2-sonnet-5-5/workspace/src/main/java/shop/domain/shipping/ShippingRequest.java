package shop.domain.shipping;

import java.math.BigDecimal;
import shop.domain.Region;

public record ShippingRequest(String zipCode, BigDecimal weightKg, Region region) {
}
