package com.mareaalta.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceTest {

    @Test
    public void testStandardPricingWithHighSeason() {
        PricingService.PricingResult result = PricingService.calculatePrice(
            "STANDARD",
            2,
            249.90,
            "2027-07-10",
            "2027-07-12",
            null
        );

        assertEquals(599.76, result.subtotal, 0.001);
        assertEquals(0, result.discount, 0.001);
        assertEquals(599.76, result.total, 0.001);
    }

    @Test
    public void testCorporateTecnosulPricing() {
        PricingService.PricingResult result = PricingService.calculatePrice(
            "CORPORATE",
            2,
            389.90,
            "2027-03-10",
            "2027-03-12",
            "TECNOSUL"
        );

        assertEquals(779.80, result.subtotal, 0.001);
        assertEquals(93.58, result.discount, 0.001);
        assertEquals(686.22, result.total, 0.001);
    }

    @Test
    public void testWeekendPackageDeluxe() {
        PricingService.PricingResult result = PricingService.calculatePrice(
            "WEEKEND_PACKAGE",
            2,
            389.90,
            "2027-03-12",
            "2027-03-14",
            null
        );

        assertEquals(820.00, result.subtotal, 0.001);
        assertEquals(0, result.discount, 0.001);
        assertEquals(820.00, result.total, 0.001);
    }

    @Test
    public void testLongStayStandardPricing() {
        PricingService.PricingResult result = PricingService.calculatePrice(
            "LONG_STAY",
            7,
            249.90,
            "2027-03-01",
            "2027-03-08",
            null
        );

        assertEquals(1749.30, result.subtotal, 0.001);
        assertEquals(174.93, result.discount, 0.001);
        assertEquals(1574.37, result.total, 0.001);
    }

    @Test
    public void testNonRefundablePricing() {
        PricingService.PricingResult result = PricingService.calculatePrice(
            "NON_REFUNDABLE",
            2,
            389.90,
            "2027-03-10",
            "2027-03-12",
            null
        );

        assertEquals(779.80, result.subtotal, 0.001);
        assertTrue(result.discount > 0);
        assertTrue(result.total < result.subtotal);
    }

    @Test
    public void testHighSeasonDetection() {
        assertTrue(PricingService.isHighSeason(java.time.LocalDate.of(2027, 7, 10)));
        assertTrue(PricingService.isHighSeason(java.time.LocalDate.of(2027, 12, 15)));
        assertTrue(PricingService.isHighSeason(java.time.LocalDate.of(2027, 1, 1)));
        assertFalse(PricingService.isHighSeason(java.time.LocalDate.of(2027, 3, 10)));
    }

    @Test
    public void testCalculateNights() {
        int nights = PricingService.calculateNights("2027-03-10", "2027-03-12");
        assertEquals(2, nights);
    }

    @Test
    public void testRoundingHalfEven() {
        double value1 = PricingService.roundToTwoCents(2.125);
        assertEquals(2.12, value1, 0.001);

        double value2 = PricingService.roundToTwoCents(2.115);
        assertEquals(2.12, value2, 0.001);
    }
}
