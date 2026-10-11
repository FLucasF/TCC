package com.example.checkout.model;

import java.math.BigDecimal;
import java.util.List;

public record Order(List<Item> items) {
    public BigDecimal subtotal() {
        return items.stream()
            .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return items.stream()
            .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
