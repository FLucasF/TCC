package shop.domain;

import java.math.BigDecimal;

public record Product(String sku, String name, BigDecimal price, BigDecimal weightKg, int stock,
                      boolean discontinued) {
}
