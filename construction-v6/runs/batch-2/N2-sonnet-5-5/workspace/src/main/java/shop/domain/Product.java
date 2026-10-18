package shop.domain;

import java.math.BigDecimal;

public record Product(String sku, String name, BigDecimal price, BigDecimal weightKg, int stock,
                      boolean discontinued) {
    public Product {
        price = Money.round(price);
        weightKg = weightKg.stripTrailingZeros();
        if (weightKg.scale() < 2) {
            weightKg = weightKg.setScale(2);
        }
    }
}
