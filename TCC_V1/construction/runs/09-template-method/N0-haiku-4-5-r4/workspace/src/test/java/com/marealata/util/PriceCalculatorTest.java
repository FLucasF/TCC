package com.marealata.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {
    @Test
    void testHighSeasonDetection() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 10)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 15)));

        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 10)));
    }

    @Test
    void testCalculateSubtotalExample() {
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(101,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12));

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testRounding() {
        BigDecimal value = new BigDecimal("249.90");
        BigDecimal result = PriceCalculator.round(value);
        assertEquals(new BigDecimal("249.90"), result);
    }
}
