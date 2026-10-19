package com.pousadamareaalta;

import com.pousadamareaalta.service.PricingService;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    public void testHighSeasonJuly() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        assertTrue(pricingService.isHighSeason(date));
    }

    @Test
    public void testHighSeasonDecember() {
        LocalDate dateAfter15 = LocalDate.of(2027, 12, 15);
        LocalDate dateBefore15 = LocalDate.of(2027, 12, 14);
        assertTrue(pricingService.isHighSeason(dateAfter15));
        assertFalse(pricingService.isHighSeason(dateBefore15));
    }

    @Test
    public void testHighSeasonJanuary() {
        LocalDate date = LocalDate.of(2027, 1, 15);
        assertTrue(pricingService.isHighSeason(date));
    }

    @Test
    public void testHighSeasonFebruary() {
        LocalDate date = LocalDate.of(2027, 2, 15);
        assertTrue(pricingService.isHighSeason(date));
    }

    @Test
    public void testLowSeasonOtherMonths() {
        LocalDate dateApril = LocalDate.of(2027, 4, 10);
        LocalDate dateAugust = LocalDate.of(2027, 8, 10);
        assertFalse(pricingService.isHighSeason(dateApril));
        assertFalse(pricingService.isHighSeason(dateAugust));
    }

    @Test
    public void testNightlyPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal price = pricingService.calculateNightlyPrice(baseRate, date);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testNightlyPriceLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 6, 10);
        BigDecimal price = pricingService.calculateNightlyPrice(baseRate, date);
        assertEquals(baseRate, price);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal nightlyPrice = new BigDecimal("299.88");
        long nights = 2;
        BigDecimal subtotal = pricingService.calculateSubtotal(nightlyPrice, nights);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, true);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, false);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testRounding() {
        BigDecimal value = new BigDecimal("249.895");
        BigDecimal rounded = pricingService.round(value);
        assertEquals(new BigDecimal("249.90"), rounded);
    }

    @Test
    public void testRoundingHalfEven() {
        BigDecimal value = new BigDecimal("0.145");
        BigDecimal rounded = pricingService.round(value);
        assertEquals(new BigDecimal("0.14"), rounded);

        BigDecimal value2 = new BigDecimal("0.155");
        BigDecimal rounded2 = pricingService.round(value2);
        assertEquals(new BigDecimal("0.16"), rounded2);
    }
}
