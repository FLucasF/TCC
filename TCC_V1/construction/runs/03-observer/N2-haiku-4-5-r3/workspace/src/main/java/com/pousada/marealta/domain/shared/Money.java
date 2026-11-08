package com.pousada.marealta.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money ofCents(long cents) {
        return new Money(BigDecimal.valueOf(cents, 2));
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor));
    }

    public Money multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    public Money applyPercent(int percent) {
        BigDecimal factor = BigDecimal.valueOf(1).add(BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100)));
        return multiply(factor);
    }

    public Money discount(int percent) {
        BigDecimal factor = BigDecimal.ONE.subtract(BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100)));
        return multiply(factor);
    }

    public long cents() {
        return amount.unscaledValue().longValue();
    }

    public BigDecimal value() {
        return amount;
    }

    public double doubleValue() {
        return amount.doubleValue();
    }

    @Override
    public String toString() {
        return String.format("R$ %.2f", amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }
}
