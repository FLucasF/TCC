package com.pousadamarealta;

import org.junit.jupiter.api.Test;
import com.pousadamarealta.utils.PricingCalculator;
import com.pousadamarealta.domain.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationSystemTest {

    @Test
    public void testHighSeasonDetection() {
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 4, 10)));
    }

    @Test
    public void testNightlyRateCalculation() {
        BigDecimal baseRate = new BigDecimal("100.00");
        BigDecimal highSeasonRate = PricingCalculator.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("120.00"), highSeasonRate);

        BigDecimal lowSeasonRate = PricingCalculator.calculateNightlyRate(baseRate, LocalDate.of(2027, 4, 10));
        assertEquals(baseRate, lowSeasonRate);
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
        BigDecimal subtotal = new BigDecimal("1000.00");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("150.00"), discount);

        BigDecimal noDiscount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, noDiscount);
    }

    @Test
    public void testNightsCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }
}
