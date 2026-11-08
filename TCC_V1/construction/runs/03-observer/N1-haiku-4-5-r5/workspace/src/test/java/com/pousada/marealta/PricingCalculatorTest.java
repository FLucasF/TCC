package com.pousada.marealta;

import com.pousada.marealta.enums.RatePlan;
import com.pousada.marealta.util.PricingCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class PricingCalculatorTest {
    @Test
    public void testHighSeasonMultiplier() {
        LocalDate julyDate = LocalDate.of(2027, 7, 10);
        assertTrue(PricingCalculator.isHighSeason(julyDate));

        LocalDate decemberDate = LocalDate.of(2027, 12, 15);
        assertTrue(PricingCalculator.isHighSeason(decemberDate));

        LocalDate januaryDate = LocalDate.of(2027, 1, 15);
        assertTrue(PricingCalculator.isHighSeason(januaryDate));

        LocalDate februaryDate = LocalDate.of(2027, 2, 15);
        assertTrue(PricingCalculator.isHighSeason(februaryDate));

        LocalDate aprilDate = LocalDate.of(2027, 4, 15);
        assertFalse(PricingCalculator.isHighSeason(aprilDate));
    }

    @Test
    public void testNightlyRateCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");

        LocalDate julyDate = LocalDate.of(2027, 7, 10);
        BigDecimal highSeasonRate = PricingCalculator.calculateNightlyRate(baseRate, julyDate);
        assertEquals(new BigDecimal("299.88"), highSeasonRate);

        LocalDate aprilDate = LocalDate.of(2027, 4, 15);
        BigDecimal normalRate = PricingCalculator.calculateNightlyRate(baseRate, aprilDate);
        assertEquals(baseRate, normalRate);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PricingCalculator.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testDiscountCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal nonRefundableDiscount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), nonRefundableDiscount);

        BigDecimal standardDiscount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, standardDiscount);
    }

    @Test
    public void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }

    @Test
    public void testNightCounting() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        int nights = PricingCalculator.countNights(checkIn, checkOut);
        assertEquals(2, nights);
    }
}
