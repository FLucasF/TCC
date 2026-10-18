package shop.catalog;

import shop.Money;

import java.math.BigDecimal;

public record Product(String sku, String name, BigDecimal price, BigDecimal weightKg, int stock, boolean discontinued) {
    public Product {
        price = Money.round(price);
        weightKg = weightKg.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}
