package com.pousada.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {

    @Test
    public void testPeakSeasonDetection() {
        assertTrue(PriceCalculator.isPeakSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PriceCalculator.isPeakSeason(LocalDate.of(2027, 12, 20)));
        assertTrue(PriceCalculator.isPeakSeason(LocalDate.of(2027, 1, 15)));
        assertFalse(PriceCalculator.isPeakSeason(LocalDate.of(2027, 5, 10)));
        assertFalse(PriceCalculator.isPeakSeason(LocalDate.of(2027, 12, 10)));
    }

    @Test
    public void testCalculateNightlyPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");

        // Low season
        BigDecimal lowSeasonPrice = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 5, 10));
        assertEquals(new BigDecimal("249.90"), lowSeasonPrice);

        // High season (20% increase)
        BigDecimal highSeasonPrice = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), highSeasonPrice);
    }

    @Test
    public void testCalculateNights() {
        int nights = PriceCalculator.calculateNights(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );
        assertEquals(2, nights);
    }

    @Test
    public void testCalculateSubtotal() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(
            baseRate,
            LocalDate.of(2027, 5, 10),
            LocalDate.of(2027, 5, 12)
        );
        assertEquals(new BigDecimal("499.80"), subtotal);
    }

    @Test
    public void testCalculateSubtotalHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(
            baseRate,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testCalculateDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal standardDiscount = PriceCalculator.calculateDiscount(subtotal, "STANDARD");
        assertEquals(BigDecimal.ZERO, standardDiscount);

        BigDecimal nonRefundableDiscount = PriceCalculator.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("89.96"), nonRefundableDiscount);
    }

    @Test
    public void testCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }
}
