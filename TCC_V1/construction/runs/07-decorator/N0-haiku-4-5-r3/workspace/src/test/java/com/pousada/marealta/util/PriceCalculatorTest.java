package com.pousada.marealta.util;

import org.junit.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class PriceCalculatorTest {
    @Test
    public void testHighSeasonJuly() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        assertTrue(PriceCalculator.isHighSeason(date));
    }

    @Test
    public void testHighSeasonDecemberToFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 15)));
    }

    @Test
    public void testNotHighSeasonMarch() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 15)));
    }

    @Test
    public void testNightlyRateHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal result = PriceCalculator.calculateNightlyRate(baseRate, date);
        assertEquals(new BigDecimal("299.88"), result);
    }

    @Test
    public void testNightlyRateLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 3, 10);
        BigDecimal result = PriceCalculator.calculateNightlyRate(baseRate, date);
        assertEquals(baseRate, result);
    }

    @Test
    public void testCalculatePrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        var price = PriceCalculator.calculatePrice(baseRate, checkIn, checkOut, "STANDARD");

        assertEquals(2, price.nights);
        assertEquals(0, new BigDecimal("299.88").compareTo(price.nightlyRate));
        assertEquals(0, new BigDecimal("599.76").compareTo(price.subtotal));
        assertEquals(0, new BigDecimal("0.00").compareTo(price.discount));
        assertEquals(0, new BigDecimal("599.76").compareTo(price.total));
    }

    @Test
    public void testCalculatePriceWithDiscount() {
        BigDecimal baseRate = new BigDecimal("389.90");
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        var price = PriceCalculator.calculatePrice(baseRate, checkIn, checkOut, "NON_REFUNDABLE");

        assertEquals(0, new BigDecimal("779.80").compareTo(price.subtotal));
        assertEquals(0, new BigDecimal("116.97").compareTo(price.discount));
        assertEquals(0, new BigDecimal("662.83").compareTo(price.total));
    }
}
