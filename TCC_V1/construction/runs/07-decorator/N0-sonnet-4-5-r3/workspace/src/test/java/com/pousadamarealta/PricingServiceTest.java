package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void shouldIdentifyHighSeasonJuly() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void shouldIdentifyHighSeasonDecemberToFebruary() {
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2028, 1, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2028, 2, 28)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2028, 3, 1)));
    }

    @Test
    void shouldCalculateNormalSeasonNightlyRate() {
        BigDecimal rate = pricingService.calculateNightlyRate(249.90, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    void shouldCalculateHighSeasonNightlyRate() {
        BigDecimal rate = pricingService.calculateNightlyRate(249.90, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void shouldCalculateSubtotalForMultipleNights() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(249.90, checkIn, checkOut);
        assertEquals(new BigDecimal("499.80"), subtotal);
    }

    @Test
    void shouldCalculateSubtotalForHighSeason() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(249.90, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    void shouldCalculateNoDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("499.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("662.83"), total);
    }
}
