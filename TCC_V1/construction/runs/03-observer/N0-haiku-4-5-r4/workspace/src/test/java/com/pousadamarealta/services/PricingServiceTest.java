package com.pousadamarealta.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceTest {
    private PricingService pricingService;

    @BeforeEach
    public void setUp() {
        pricingService = new PricingService();
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 15)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 2, 15)));
    }

    @Test
    public void testHighSeasonJuly() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 15)));
    }

    @Test
    public void testLowSeasonMarch() {
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 3, 15)));
    }

    @Test
    public void testNightlyRateHighSeason() {
        BigDecimal baseRate = BigDecimal.valueOf(249.90);
        BigDecimal highSeasonRate = pricingService.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(0, new BigDecimal("299.88").compareTo(highSeasonRate));
    }

    @Test
    public void testNightlyRateLowSeason() {
        BigDecimal baseRate = BigDecimal.valueOf(249.90);
        BigDecimal lowSeasonRate = pricingService.calculateNightlyRate(baseRate, LocalDate.of(2027, 3, 10));
        assertEquals(0, new BigDecimal("249.90").compareTo(lowSeasonRate));
    }

    @Test
    public void testSubtotalTwoNights() {
        BigDecimal baseRate = BigDecimal.valueOf(249.90);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(0, new BigDecimal("599.76").compareTo(subtotal));
    }

    @Test
    public void testDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, com.pousadamarealta.models.RatePlan.NON_REFUNDABLE);
        assertEquals(0, new BigDecimal("89.96").compareTo(discount));
    }

    @Test
    public void testDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, com.pousadamarealta.models.RatePlan.STANDARD);
        assertEquals(0, BigDecimal.ZERO.compareTo(discount));
    }

    @Test
    public void testTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        assertEquals(0, new BigDecimal("509.80").compareTo(total));
    }

    @Test
    public void testCountNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        int nights = pricingService.countNights(checkIn, checkOut);
        assertEquals(2, nights);
    }

    @Test
    public void testRoundingHalfEven() {
        BigDecimal baseRate = BigDecimal.valueOf(389.90);
        BigDecimal highSeasonRate = pricingService.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(0, new BigDecimal("467.88").compareTo(highSeasonRate));
    }
}
