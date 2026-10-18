package shop.domain;

import java.math.BigDecimal;

public record CartLine(Product product, int quantity) {
    public BigDecimal total() {
        return product.price().multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal weightKg() {
        return product.weightKg().multiply(BigDecimal.valueOf(quantity));
    }
}
