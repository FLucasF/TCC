package com.loja.domain;

import java.math.BigDecimal;

public record Weight(BigDecimal kilograms) {
    public Weight {
        if (kilograms == null || kilograms.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Weight must be non-negative");
        }
    }

    public Weight(double kg) {
        this(BigDecimal.valueOf(kg));
    }

    public Weight add(Weight other) {
        return new Weight(this.kilograms.add(other.kilograms));
    }

    public Weight multiply(int quantity) {
        return new Weight(this.kilograms.multiply(BigDecimal.valueOf(quantity)));
    }

    public int toGrams() {
        return this.kilograms.multiply(BigDecimal.valueOf(1000)).intValue();
    }

    public boolean isGreaterThan(Weight other) {
        return this.kilograms.compareTo(other.kilograms) > 0;
    }
}
