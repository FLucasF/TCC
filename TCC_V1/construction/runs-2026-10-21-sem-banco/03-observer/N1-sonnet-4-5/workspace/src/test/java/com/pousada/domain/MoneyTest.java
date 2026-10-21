package com.pousada.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test
    void shouldRoundHalfEven() {
        Money money1 = new Money(new BigDecimal("10.125"));
        assertEquals("10.12", money1.getAmount().toString());

        Money money2 = new Money(new BigDecimal("10.135"));
        assertEquals("10.14", money2.getAmount().toString());

        Money money3 = new Money(new BigDecimal("10.145"));
        assertEquals("10.14", money3.getAmount().toString());

        Money money4 = new Money(new BigDecimal("10.155"));
        assertEquals("10.16", money4.getAmount().toString());
    }

    @Test
    void shouldMultiply() {
        Money base = new Money(new BigDecimal("249.90"));
        Money result = base.multiply(new BigDecimal("1.20"));
        assertEquals("299.88", result.getAmount().toString());
    }

    @Test
    void shouldAdd() {
        Money m1 = new Money(new BigDecimal("299.88"));
        Money m2 = new Money(new BigDecimal("299.88"));
        Money result = m1.add(m2);
        assertEquals("599.76", result.getAmount().toString());
    }

    @Test
    void shouldFormatBRL() {
        Money money = new Money(new BigDecimal("1234.56"));
        assertTrue(money.formatBRL().contains("1.234,56"));
    }
}
