package com.loja.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal value) {
    public Money {
        if (value == null) {
            throw new IllegalArgumentException("Money value cannot be null");
        }
        value = value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Money(String value) {
        this(new BigDecimal(value));
    }

    public Money(double value) {
        this(BigDecimal.valueOf(value));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(this.value.add(other.value));
    }

    public Money subtract(Money other) {
        return new Money(this.value.subtract(other.value));
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.value.multiply(factor));
    }

    public Money multiply(int factor) {
        return new Money(this.value.multiply(BigDecimal.valueOf(factor)));
    }

    public Money divide(int divisor) {
        return new Money(this.value.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_EVEN));
    }

    public Money percentage(BigDecimal percentage) {
        return new Money(this.value.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN));
    }

    public Money min(Money other) {
        return this.value.compareTo(other.value) <= 0 ? this : other;
    }

    public boolean isGreaterThan(Money other) {
        return this.value.compareTo(other.value) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.value.compareTo(other.value) >= 0;
    }

    public boolean isLessThan(Money other) {
        return this.value.compareTo(other.value) < 0;
    }

    public boolean isZero() {
        return this.value.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.value.compareTo(BigDecimal.ZERO) > 0;
    }

    public Money negate() {
        return new Money(this.value.negate());
    }
}
