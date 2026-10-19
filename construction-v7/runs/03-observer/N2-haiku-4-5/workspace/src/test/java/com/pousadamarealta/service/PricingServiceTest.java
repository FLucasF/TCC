package com.pousadamarealta.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceTest {
    private final PricingService service = new PricingService();

    @Test
    public void testHighSeasonDates() {
        assertTrue(service.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(service.isHighSeason(LocalDate.of(2027, 12, 20)));
        assertTrue(service.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(service.isHighSeason(LocalDate.of(2027, 2, 10)));

        assertFalse(service.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(service.isHighSeason(LocalDate.of(2027, 6, 10)));
    }

    @Test
    public void testNightlyRateWithoutHighSeason() {
        BigDecimal base = new BigDecimal("249.90");
        BigDecimal rate = service.calculateNightlyRate(base, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    public void testNightlyRateWithHighSeason() {
        BigDecimal base = new BigDecimal("249.90");
        BigDecimal rate = service.calculateNightlyRate(base, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    public void testSubtotalHighSeason() {
        BigDecimal base = new BigDecimal("249.90");
        BigDecimal subtotal = service.calculateSubtotal(base, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = service.calculateDiscount(subtotal, true);
        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    public void testDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = service.calculateDiscount(subtotal, false);
        assertEquals(0, discount.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testTotal() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal total = service.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("662.83"), total);
    }

    @Test
    public void testCountNights() {
        int nights = service.countNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }

    @Test
    public void testRounding() {
        BigDecimal value = new BigDecimal("249.895");
        BigDecimal rounded = PricingService.round(value);
        assertEquals(new BigDecimal("249.90"), rounded);
    }
}
