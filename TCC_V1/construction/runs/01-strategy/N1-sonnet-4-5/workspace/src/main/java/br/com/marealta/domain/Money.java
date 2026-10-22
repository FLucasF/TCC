package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier));
    }

    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    public Money discount(BigDecimal percentage) {
        BigDecimal discountAmount = this.amount.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
        return new Money(discountAmount);
    }

    public Money applyDiscount(BigDecimal percentage) {
        Money discountAmount = discount(percentage);
        return subtract(discountAmount);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
