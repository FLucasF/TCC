package com.pousadamarealta;

import static org.junit.jupiter.api.Assertions.*;

import com.pousadamarealta.util.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    void testRounding() {
        BigDecimal value = new BigDecimal("1.234");
        BigDecimal rounded = Money.round(value);
        assertEquals(new BigDecimal("1.23"), rounded);
    }

    @Test
    void testHalfEvenRounding() {
        BigDecimal value1 = new BigDecimal("1.245");
        BigDecimal rounded1 = Money.round(value1);
        assertEquals(new BigDecimal("1.24"), rounded1);

        BigDecimal value2 = new BigDecimal("1.235");
        BigDecimal rounded2 = Money.round(value2);
        assertEquals(new BigDecimal("1.24"), rounded2);
    }

    @Test
    void testPercent() {
        BigDecimal value = new BigDecimal("100.00");
        BigDecimal percent15 = Money.percent(value, 15);
        assertEquals(new BigDecimal("15.00"), percent15);
    }

    @Test
    void testMultiply() {
        BigDecimal value = new BigDecimal("249.90");
        BigDecimal multiplied = Money.multiply(value, new BigDecimal("1.20"));
        assertEquals(new BigDecimal("299.88"), multiplied);
    }
}
