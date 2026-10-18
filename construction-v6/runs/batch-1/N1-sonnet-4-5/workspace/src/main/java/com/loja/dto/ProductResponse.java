package com.loja.dto;

import com.loja.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(
    String sku,
    String name,
    BigDecimal price,
    BigDecimal weightKg,
    int stock,
    boolean discontinued
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getSku(),
            product.getName(),
            product.getPrice().value(),
            product.getWeight().kilograms(),
            product.getStock(),
            product.isDiscontinued()
        );
    }
}
