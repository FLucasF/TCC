package com.marealta.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingServiceTest {

    @Test
    public void testHighSeasonDetection() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 2, 15)));

        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testCorporateDiscountTecnosul() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal baseRate = new BigDecimal("389.90");

        PricingService.PricingResult result = PricingService.calculatePrice(
            checkIn, checkOut, baseRate, "CORPORATE", "TECNOSUL"
        );

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("779.80"), result.subtotal);
        assertEquals(new BigDecimal("93.58"), result.discount);
        assertEquals(new BigDecimal("686.22"), result.total);
    }

    @Test
    public void testLongStayDiscount() {
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);
        BigDecimal baseRate = new BigDecimal("249.90");

        PricingService.PricingResult result = PricingService.calculatePrice(
            checkIn, checkOut, baseRate, "LONG_STAY", null
        );

        assertEquals(7, result.nights);
        assertEquals(new BigDecimal("1749.30"), result.subtotal);
        assertEquals(new BigDecimal("174.93"), result.discount);
        assertEquals(new BigDecimal("1574.37"), result.total);
    }

    @Test
    public void testHighSeasonWithLongStay() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 17);
        BigDecimal baseRate = new BigDecimal("249.90");

        PricingService.PricingResult result = PricingService.calculatePrice(
            checkIn, checkOut, baseRate, "LONG_STAY", null
        );

        assertEquals(7, result.nights);
        // Each night: 249.90 × 1.20 = 299.88
        // Total nights: 7 × 299.88 = 2099.16
        // Discount: 2099.16 × 0.10 = 209.916 → 209.92
        // Total: 2099.16 - 209.92 = 1889.24
        assertEquals(new BigDecimal("2099.16"), result.subtotal);
        assertEquals(new BigDecimal("209.92"), result.discount);
        assertEquals(new BigDecimal("1889.24"), result.total);
    }
}
