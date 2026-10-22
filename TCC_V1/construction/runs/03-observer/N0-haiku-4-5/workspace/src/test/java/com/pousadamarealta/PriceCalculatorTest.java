package com.pousadamarealta;

import com.pousadamarealta.utils.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {
    @Test
    public void testPeakSeasonDetection() {
        assertTrue(PriceCalculator.isPeakSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PriceCalculator.isPeakSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isPeakSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(PriceCalculator.isPeakSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PriceCalculator.isPeakSeason(LocalDate.of(2027, 2, 28)));

        assertFalse(PriceCalculator.isPeakSeason(LocalDate.of(2027, 12, 14)));
        assertFalse(PriceCalculator.isPeakSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(PriceCalculator.isPeakSeason(LocalDate.of(2027, 6, 15)));
    }

    @Test
    public void testNightlyRateCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal peakRate = PriceCalculator.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(0, peakRate.compareTo(new BigDecimal("299.88")));

        BigDecimal normalRate = PriceCalculator.calculateNightlyRate(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(0, normalRate.compareTo(new BigDecimal("249.90")));
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(0, subtotal.compareTo(new BigDecimal("599.76")));
    }

    @Test
    public void testDiscountCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discountNonRefundable = PriceCalculator.calculateDiscount(subtotal, true);
        assertEquals(0, discountNonRefundable.compareTo(new BigDecimal("89.96")));

        BigDecimal discountStandard = PriceCalculator.calculateDiscount(subtotal, false);
        assertEquals(0, discountStandard.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);
        assertEquals(0, total.compareTo(new BigDecimal("509.80")));
    }

    @Test
    public void testNightsCount() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        int nights = PriceCalculator.countNights(checkIn, checkOut);
        assertEquals(2, nights);
    }
}
