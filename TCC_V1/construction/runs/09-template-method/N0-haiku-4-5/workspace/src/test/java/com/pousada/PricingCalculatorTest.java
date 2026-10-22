package com.pousada;

import com.pousada.model.RatePlan;
import com.pousada.service.PricingCalculator;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingCalculatorTest {

    @Test
    public void testHighSeasonMultiplier() {
        // July 10, 2027 - high season
        LocalDate julDate = LocalDate.of(2027, 7, 10);
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal price = PricingCalculator.calculateNightlyPrice(baseRate, julDate);

        // 249.90 * 1.20 = 299.88
        assertEquals(0, new BigDecimal("299.88").compareTo(price));
    }

    @Test
    public void testLowSeasonRate() {
        // May 10, 2027 - low season
        LocalDate mayDate = LocalDate.of(2027, 5, 10);
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal price = PricingCalculator.calculateNightlyPrice(baseRate, mayDate);

        assertEquals(0, baseRate.compareTo(price));
    }

    @Test
    public void testDecemberHighSeason() {
        // December 15, 2027 - high season starts
        LocalDate dec15 = LocalDate.of(2027, 12, 15);
        BigDecimal baseRate = new BigDecimal("389.90");

        BigDecimal price = PricingCalculator.calculateNightlyPrice(baseRate, dec15);

        // 389.90 * 1.20 = 467.88
        assertEquals(0, new BigDecimal("467.88").compareTo(price));
    }

    @Test
    public void testDecember14LowSeason() {
        // December 14, 2027 - still low season
        LocalDate dec14 = LocalDate.of(2027, 12, 14);
        BigDecimal baseRate = new BigDecimal("389.90");

        BigDecimal price = PricingCalculator.calculateNightlyPrice(baseRate, dec14);

        assertEquals(0, baseRate.compareTo(price));
    }

    @Test
    public void testNightCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        int nights = PricingCalculator.calculateNights(checkIn, checkOut);

        assertEquals(2, nights);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal nightlyPrice = new BigDecimal("299.88");
        int nights = 2;

        BigDecimal subtotal = PricingCalculator.calculateSubtotal(nightlyPrice, nights);

        assertEquals(0, new BigDecimal("599.76").compareTo(subtotal));
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        // 599.76 * 0.15 = 89.964, rounded to 89.96
        assertEquals(0, new BigDecimal("89.96").compareTo(discount));
    }

    @Test
    public void testStandardNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(0, BigDecimal.ZERO.compareTo(discount));
    }
}
