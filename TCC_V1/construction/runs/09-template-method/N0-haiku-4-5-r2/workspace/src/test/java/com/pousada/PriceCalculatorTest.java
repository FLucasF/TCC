package com.pousada;

import com.pousada.utils.DateUtils;
import com.pousada.utils.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PriceCalculatorTest {
    @Test
    public void testStandardPlanLowSeason() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal rate = new BigDecimal("249.90");

        PriceCalculator.PriceResult result = PriceCalculator.calculate(rate, checkIn, checkOut, "STANDARD");

        assertEquals(0, new BigDecimal("249.90").compareTo(result.nightlyRate));
        assertEquals(0, new BigDecimal("499.80").compareTo(result.subtotal));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.discount));
        assertEquals(0, new BigDecimal("499.80").compareTo(result.total));
    }

    @Test
    public void testStandardPlanHighSeason() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal rate = new BigDecimal("249.90");

        PriceCalculator.PriceResult result = PriceCalculator.calculate(rate, checkIn, checkOut, "STANDARD");

        assertEquals(0, new BigDecimal("299.88").compareTo(result.nightlyRate));
        assertEquals(0, new BigDecimal("599.76").compareTo(result.subtotal));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.discount));
        assertEquals(0, new BigDecimal("599.76").compareTo(result.total));
    }

    @Test
    public void testNonRefundablePlan() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal rate = new BigDecimal("389.90");

        PriceCalculator.PriceResult result = PriceCalculator.calculate(rate, checkIn, checkOut, "NON_REFUNDABLE");

        assertEquals(0, new BigDecimal("389.90").compareTo(result.nightlyRate));
        assertEquals(0, new BigDecimal("779.80").compareTo(result.subtotal));
        assertEquals(0, new BigDecimal("116.97").compareTo(result.discount));
        assertEquals(0, new BigDecimal("662.83").compareTo(result.total));
    }

    @Test
    public void testDateParsing() {
        LocalDate date = DateUtils.parseDate("2027-03-10");
        assertEquals(2027, date.getYear());
        assertEquals(3, date.getMonthValue());
        assertEquals(10, date.getDayOfMonth());
    }

    @Test
    public void testHighSeasonDetection() {
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(DateUtils.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(DateUtils.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    private void assertTrue(boolean value) {
        assertEquals(true, value);
    }

    private void assertFalse(boolean value) {
        assertEquals(false, value);
    }
}
