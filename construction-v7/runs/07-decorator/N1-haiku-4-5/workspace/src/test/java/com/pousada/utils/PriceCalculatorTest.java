package com.pousada.utils;

import com.pousada.models.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class PriceCalculatorTest {
    @Test
    public void testLowSeasonRate() {
        BigDecimal rate = PriceCalculator.calculateNightlyRate(LocalDate.of(2027, 3, 10), new BigDecimal("249.90"));
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    public void testHighSeasonRate() {
        BigDecimal rate = PriceCalculator.calculateNightlyRate(LocalDate.of(2027, 7, 10), new BigDecimal("249.90"));
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    public void testHighSeasonRateDecember() {
        BigDecimal rate = PriceCalculator.calculateNightlyRate(LocalDate.of(2027, 12, 25), new BigDecimal("249.90"));
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                new BigDecimal("249.90"));
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testStandardPlanDiscount() {
        BigDecimal discount = PriceCalculator.calculateDiscount(new BigDecimal("100.00"), RatePlan.STANDARD);
        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal discount = PriceCalculator.calculateDiscount(new BigDecimal("100.00"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("15.00"), discount);
    }

    @Test
    public void testTotalCalculation() {
        BigDecimal total = PriceCalculator.calculateTotal(new BigDecimal("100.00"), new BigDecimal("15.00"));
        assertEquals(new BigDecimal("85.00"), total);
    }
}
