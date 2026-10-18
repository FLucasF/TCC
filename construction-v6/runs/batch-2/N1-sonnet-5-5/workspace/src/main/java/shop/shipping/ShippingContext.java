package shop.shipping;

import shop.checkout.Region;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ShippingContext(String zipCode, Region region, BigDecimal weightKg) {
    public int weightGrams() {
        return weightKg.movePointRight(3).setScale(0, RoundingMode.CEILING).intValueExact();
    }
}
