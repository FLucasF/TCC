package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class Money {
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);

    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public Money(String amount) {
        this(new BigDecimal(amount));
    }

    public static Money round(BigDecimal value) {
        return new Money(value);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier));
    }

    public Money multiply(int multiplier) {
        return multiply(new BigDecimal(multiplier));
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String formatBRL() {
        return CURRENCY_FORMAT.format(amount);
    }

    @Override
    public String toString() {
        return amount.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }
}
