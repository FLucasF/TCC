package com.pousadamarealta;

import com.pousadamarealta.service.PricingService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class PricingServiceTest {
    private PricingService pricingService;

    @Before
    public void setUp() {
        pricingService = new PricingService();
    }

    @Test
    public void testNormalSeasonRate() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 3, 10);
        BigDecimal result = pricingService.calculateNightlyRate(baseRate, date);
        assertEquals(baseRate, result);
    }

    @Test
    public void testHighSeasonDecemberRate() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 12, 20);
        BigDecimal result = pricingService.calculateNightlyRate(baseRate, date);
        BigDecimal expected = new BigDecimal("299.88");
        assertEquals(expected, result);
    }

    @Test
    public void testHighSeasonJulyRate() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal result = pricingService.calculateNightlyRate(baseRate, date);
        BigDecimal expected = new BigDecimal("299.88");
        assertEquals(expected, result);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal result = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);
        BigDecimal expected = new BigDecimal("599.76");
        assertEquals(expected, result);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal result = pricingService.calculateDiscount(subtotal, "NON_REFUNDABLE");
        BigDecimal expected = new BigDecimal("89.96");
        assertEquals(expected, result);
    }

    @Test
    public void testStandardPlanDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal result = pricingService.calculateDiscount(subtotal, "STANDARD");
        BigDecimal expected = BigDecimal.ZERO;
        assertEquals(expected, result);
    }

    @Test
    public void testIssRetention() {
        BigDecimal total = new BigDecimal("779.80");
        BigDecimal result = pricingService.calculateIssRetention(total);
        BigDecimal expected = new BigDecimal("38.99");
        assertEquals(expected, result);
    }
}
