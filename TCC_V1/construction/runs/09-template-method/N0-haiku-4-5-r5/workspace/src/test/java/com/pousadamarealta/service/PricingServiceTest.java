package com.pousadamarealta.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
    @Test
    void testHighSeasonDetection() {
        // December 15 onwards
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 12, 14)));

        // January
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 1, 1)));

        // February
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 2, 28)));

        // July
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 7, 31)));

        // Other months
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 8, 10)));
    }

    @Test
    void testNightlyRateCalculation() {
        BigDecimal standardRate = new BigDecimal("249.90");

        // Low season
        BigDecimal lowSeason = PricingService.calculateNightlyRate(standardRate, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), lowSeason);

        // High season (20% more)
        BigDecimal highSeason = PricingService.calculateNightlyRate(standardRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), highSeason);
    }

    @Test
    void testSubtotalCalculation() {
        // Example from spec: 10/07/2027 to 12/07/2027, STANDARD room (249.90)
        // High season: 249.90 * 1.20 = 299.88 per night
        // 2 nights: 599.76
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal subtotal = PricingService.calculateSubtotal(rate,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12));
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testDiscountCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");

        // Standard plan - no discount
        BigDecimal standardDiscount = PricingService.calculateDiscount(subtotal, "STANDARD");
        assertEquals(new BigDecimal("0.00"), standardDiscount);

        // Non-refundable plan - 15% discount
        BigDecimal nonRefundableDiscount = PricingService.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(new BigDecimal("89.96"), nonRefundableDiscount);
    }

    @Test
    void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = PricingService.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }

    @Test
    void testNightsCalculation() {
        int nights = PricingService.calculateNights(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);

        int oneNight = PricingService.calculateNights(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 11));
        assertEquals(1, oneNight);
    }

    @Test
    void testRoundingHalfEven() {
        // Test that HALF_EVEN rounding is applied
        BigDecimal testValue = new BigDecimal("389.90");
        BigDecimal highSeason = PricingService.calculateNightlyRate(testValue, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("467.88"), highSeason);
    }
}
