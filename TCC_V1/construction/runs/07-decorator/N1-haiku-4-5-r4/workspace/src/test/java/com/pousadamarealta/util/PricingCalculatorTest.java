package com.pousadamarealta.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.pousadamarealta.models.RatePlan;
import static org.junit.jupiter.api.Assertions.*;

public class PricingCalculatorTest {
    @Test
    public void testHighSeasonJuly() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate julDate = LocalDate.of(2027, 7, 10);
        BigDecimal nightlyRate = PricingCalculator.calculateNightlyRate(rate, julDate);
        assertEquals(new BigDecimal("299.88"), nightlyRate);
    }

    @Test
    public void testHighSeasonDecember() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate decDate = LocalDate.of(2027, 12, 15);
        BigDecimal nightlyRate = PricingCalculator.calculateNightlyRate(rate, decDate);
        assertEquals(new BigDecimal("299.88"), nightlyRate);
    }

    @Test
    public void testHighSeasonJanuary() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate janDate = LocalDate.of(2027, 1, 15);
        BigDecimal nightlyRate = PricingCalculator.calculateNightlyRate(rate, janDate);
        assertEquals(new BigDecimal("299.88"), nightlyRate);
    }

    @Test
    public void testHighSeasonFebruary() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate febDate = LocalDate.of(2027, 2, 15);
        BigDecimal nightlyRate = PricingCalculator.calculateNightlyRate(rate, febDate);
        assertEquals(new BigDecimal("299.88"), nightlyRate);
    }

    @Test
    public void testLowSeason() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate lowDate = LocalDate.of(2027, 3, 15);
        BigDecimal nightlyRate = PricingCalculator.calculateNightlyRate(rate, lowDate);
        assertEquals(rate, nightlyRate);
    }

    @Test
    public void testNightCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }
}
